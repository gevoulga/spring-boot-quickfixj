/*
 * Copyright (c) 2020 Georgios Voulgarakis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.voulgarakis.spring.boot.starter.quickfixj.session.settings;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Dictionary;
import quickfix.*;
import quickfix.field.converter.BooleanConverter;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Settings for sessions. Settings are grouped by FIX version and target company
 * ID. There is also a default settings section that is inherited by the
 * session-specific sections.
 * <p>
 * Setting constants are declared in the classes using the settings. To find the
 * string constants, navigate to the class constant for the setting, select the
 * link for the setting and then and select the "Constant Field Values" link in
 * the detailed field description.
 *
 * @see Acceptor
 * @see Initiator
 * @see FileLogFactory
 * @see ScreenLogFactory
 * @see FileStoreFactory
 * @see JdbcSetting
 * @see Session
 * @see DefaultSessionFactory
 */
public class BaseSessionSettings extends SessionSettings {
    private static final Logger log = LoggerFactory.getLogger(BaseSessionSettings.class);

    private static final SessionID DEFAULT_SESSION_ID = new SessionID("DEFAULT", "", "");
    private static final String SESSION_SECTION_NAME = "session";
    private static final String DEFAULT_SECTION_NAME = "default";
    public static final String BEGINSTRING = "BeginString";
    public static final String SENDERCOMPID = "SenderCompID";
    public static final String SENDERSUBID = "SenderSubID";
    public static final String SENDERLOCID = "SenderLocationID";
    public static final String TARGETCOMPID = "TargetCompID";
    public static final String TARGETSUBID = "TargetSubID";
    public static final String TARGETLOCID = "TargetLocationID";
    public static final String SESSION_QUALIFIER = "SessionQualifier";

    // This was using the line.separator system property but that caused
    // problems with moving configuration files between *nix and Windows.
    private static final String NEWLINE = "\r\n";

    private Properties variableValues = System.getProperties();

    /**
     * Creates an empty session settings object.
     */
    public BaseSessionSettings() {
        sections.put(DEFAULT_SESSION_ID, new Properties());
    }

    /**
     * Loads session settings from a file.
     *
     * @param filename the path to the file containing the session settings
     */
    public BaseSessionSettings(String filename) throws ConfigError {
        this();
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        if (in == null) {
            try {
                in = new FileInputStream(filename);
            } catch (IOException e) {
                throw new ConfigError(e.getMessage());
            }
        }
        load(in);
    }

    /**
     * Loads session settings from an input stream.
     *
     * @param stream the input stream
     * @throws ConfigError
     */
    public BaseSessionSettings(InputStream stream) throws ConfigError {
        this();
        load(stream);
    }

    /**
     * Gets a string from the default section of the settings.
     *
     * @param key
     * @return the default string value
     * @throws ConfigError
     */
    @Override
    public String getString(String key) throws ConfigError {
        return getString(DEFAULT_SESSION_ID, key);
    }

    /**
     * Get a settings string.
     *
     * @param sessionID the session ID
     * @param key       the settings key
     * @return the string value for the setting
     * @throws ConfigError configuration error, probably a missing setting.
     */
    @Override
    public String getString(SessionID sessionID, String key) throws ConfigError {
        String value = interpolate(getSessionProperties(sessionID).getProperty(key));
        if (value == null) {
            throw new ConfigError(key + " not defined");
        }
        return value;
    }

    /**
     * Return the settings for a session as a Properties object.
     *
     * @param sessionID
     * @param includeDefaults if true, include settings defaults in properties
     * @return the Properties object with the session settings
     * @throws ConfigError
     * @see Properties
     */
    @Override
    public Properties getSessionProperties(SessionID sessionID, boolean includeDefaults)
            throws ConfigError {
        Properties p = sections.get(sessionID);
        if (p == null) {
            throw new ConfigError("Session not found");
        }
        if (includeDefaults) {
            Properties mergedProperties = new Properties();
            mergedProperties.putAll(sections.get(DEFAULT_SESSION_ID));
            mergedProperties.putAll(p);
            return mergedProperties;
        } else {
            return p;
        }
    }

    /**
     * Return the settings for a session as a Properties object.
     *
     * @param sessionID
     * @return the Properties object with the session settings
     * @throws ConfigError
     * @see Properties
     */
    @Override
    public Properties getSessionProperties(SessionID sessionID) throws ConfigError {
        return getSessionProperties(sessionID, false);
    }

    /**
     * Returns the defaults for the session-level settings.
     *
     * @return the default properties
     * @throws ConfigError
     */
    @Override
    public Properties getDefaultProperties() {
        try {
            return getSessionProperties(DEFAULT_SESSION_ID);
        } catch (ConfigError e) {
            // shouldn't happen
            return new Properties();
        }
    }

    /**
     * Gets a long from the default section of settings.
     *
     * @param key
     * @return the default value
     * @throws ConfigError
     * @throws FieldConvertError
     */
    @Override
    public long getLong(String key) throws ConfigError, FieldConvertError {
        return getLong(DEFAULT_SESSION_ID, key);
    }

    /**
     * Get a settings value as a long integer.
     *
     * @param sessionID the session ID
     * @param key       the settings key
     * @return the long integer value for the setting
     * @throws ConfigError       configuration error, probably a missing setting.
     * @throws FieldConvertError error during field type conversion.
     */
    @Override
    public long getLong(SessionID sessionID, String key) throws ConfigError, FieldConvertError {
        try {
            return Long.parseLong(getString(sessionID, key));
        } catch (NumberFormatException e) {
            throw new FieldConvertError(e.getMessage());
        }
    }

    /**
     * Gets an int from the default section of settings.
     *
     * @param key
     * @return the default value
     * @throws ConfigError
     * @throws FieldConvertError
     */
    @Override
    public int getInt(String key) throws ConfigError, FieldConvertError {
        return getInt(DEFAULT_SESSION_ID, key);
    }

    /**
     * Get a settings value as an integer.
     *
     * @param sessionID the session ID
     * @param key       the settings key
     * @return the long integer value for the setting
     * @throws ConfigError       configurion error, probably a missing setting.
     * @throws FieldConvertError error during field type conversion.
     */
    @Override
    public int getInt(SessionID sessionID, String key) throws ConfigError, FieldConvertError {
        try {
            return Integer.parseInt(getString(sessionID, key));
        } catch (NumberFormatException e) {
            throw new FieldConvertError(e.getMessage());
        }
    }

    private Properties getOrCreateSessionProperties(SessionID sessionID) {
        return sections.computeIfAbsent(sessionID, k -> new Properties(sections.get(DEFAULT_SESSION_ID)));
    }

    /**
     * Gets a double value from the default section of the settings.
     *
     * @param key
     * @return the default value
     * @throws ConfigError
     * @throws FieldConvertError
     */
    @Override
    public double getDouble(String key) throws ConfigError, FieldConvertError {
        return getDouble(DEFAULT_SESSION_ID, key);
    }

    /**
     * Get a settings value as a double number.
     *
     * @param sessionID the session ID
     * @param key       the settings key
     * @return the double number value for the setting
     * @throws ConfigError       configuration error, probably a missing setting.
     * @throws FieldConvertError error during field type conversion.
     */
    @Override
    public double getDouble(SessionID sessionID, String key) throws ConfigError, FieldConvertError {
        try {
            return Double.parseDouble(getString(sessionID, key));
        } catch (NumberFormatException e) {
            throw new FieldConvertError(e.getMessage());
        }
    }

    /**
     * Gets a boolean value from the default section of the settings.
     *
     * @param key
     * @return the boolean value
     * @throws ConfigError
     * @throws FieldConvertError
     */
    @Override
    public boolean getBool(String key) throws ConfigError, FieldConvertError {
        return getBool(DEFAULT_SESSION_ID, key);
    }

    /**
     * Get a settings value as a boolean value.
     *
     * @param sessionID the session ID
     * @param key       the settings key
     * @return the boolean value for the setting
     * @throws ConfigError       configuration error, probably a missing setting.
     * @throws FieldConvertError error during field type conversion.
     */
    @Override
    public boolean getBool(SessionID sessionID, String key) throws ConfigError, FieldConvertError {
        try {
            return BooleanConverter.convert(getString(sessionID, key));
        } catch (FieldConvertError e) {
            throw new ConfigError(e);
        }
    }

    /**
     * Sets a string-valued session setting.
     *
     * @param sessionID the session ID
     * @param key       the setting key
     * @param value     the string value
     */
    @Override
    public void setString(SessionID sessionID, String key, String value) {
        getOrCreateSessionProperties(sessionID).setProperty(key, value.trim());
    }

    /**
     * Sets a long integer-valued session setting.
     *
     * @param sessionID the session ID
     * @param key       the setting key
     * @param value     the long integer value
     */
    @Override
    public void setLong(SessionID sessionID, String key, long value) {
        getOrCreateSessionProperties(sessionID).setProperty(key, Long.toString(value));
    }

    /**
     * Sets a double-valued session setting.
     *
     * @param sessionID the session ID
     * @param key       the setting key
     * @param value     the double value
     */
    @Override
    public void setDouble(SessionID sessionID, String key, double value) {
        getOrCreateSessionProperties(sessionID).setProperty(key, Double.toString(value));
    }

    /**
     * Sets a boolean-valued session setting.
     *
     * @param sessionID the session ID
     * @param key       the setting key
     * @param value     the boolean value
     */
    @Override
    public void setBool(SessionID sessionID, String key, boolean value) {
        getOrCreateSessionProperties(sessionID).setProperty(key, BooleanConverter.convert(value));
    }

    private final ConcurrentMap<SessionID, Properties> sections = new ConcurrentHashMap<>();

    @Override
    public Iterator<SessionID> sectionIterator() {
        HashSet<SessionID> nondefaultSessions = new HashSet<>(sections.keySet());
        nondefaultSessions.remove(DEFAULT_SESSION_ID);
        return nondefaultSessions.iterator();
    }

    private void load(InputStream inputStream) throws ConfigError {
        try {
            Properties currentSection = null;
            String currentSectionId = null;
            Tokenizer tokenizer = new Tokenizer();
            Reader reader = new InputStreamReader(inputStream);
            Tokenizer.Token token = tokenizer.getToken(reader);
            while (token != null) {
                if (token.getType() == Tokenizer.SECTION_TOKEN) {
                    storeSection(currentSectionId, currentSection);
                    if (token.getValue().equalsIgnoreCase(DEFAULT_SECTION_NAME)) {
                        currentSectionId = DEFAULT_SECTION_NAME;
                        currentSection = getSessionProperties(DEFAULT_SESSION_ID);
                    } else if (token.getValue().equalsIgnoreCase(SESSION_SECTION_NAME)) {
                        currentSectionId = SESSION_SECTION_NAME;
                        currentSection = new Properties(getSessionProperties(DEFAULT_SESSION_ID));
                    }
                } else if (token.getType() == Tokenizer.ID_TOKEN) {
                    Tokenizer.Token valueToken = tokenizer.getToken(reader);
                    if (currentSection != null) {
                        String value = interpolate(valueToken.getValue());
                        currentSection.put(token.getValue(), value);
                    }
                }
                token = tokenizer.getToken(reader);
            }
            storeSection(currentSectionId, currentSection);
        } catch (IOException e) {
            ConfigError configError = new ConfigError(e.getMessage());
            configError.fillInStackTrace();
            throw configError;
        }
    }

    private void storeSection(String currentSectionId, Properties currentSection) {
        if (currentSectionId != null && currentSectionId.equals(SESSION_SECTION_NAME)) {
            SessionID sessionId = new SessionID(currentSection.getProperty(BEGINSTRING),
                    currentSection.getProperty(SENDERCOMPID),
                    currentSection.getProperty(SENDERSUBID),
                    currentSection.getProperty(SENDERLOCID),
                    currentSection.getProperty(TARGETCOMPID),
                    currentSection.getProperty(TARGETSUBID),
                    currentSection.getProperty(TARGETLOCID),
                    currentSection.getProperty(SESSION_QUALIFIER));
            sections.put(sessionId, currentSection);
        }
    }

    /**
     * Predicate for determining if a setting is in the default section.
     *
     * @param key
     * @return true if setting is in the defaults, false otherwise
     */
    @Override
    public boolean isSetting(String key) {
        return isSetting(DEFAULT_SESSION_ID, key);
    }

    /**
     * Predicate for determining if a setting exists.
     *
     * @param sessionID the session ID
     * @param key       the setting key
     * @return true is setting exists, false otherwise.
     */
    @Override
    public boolean isSetting(SessionID sessionID, String key) {
        return getOrCreateSessionProperties(sessionID).getProperty(key) != null;
    }

    @Override
    public void removeSetting(SessionID sessionID, String key) {
        getOrCreateSessionProperties(sessionID).remove(key);
    }

    private static class Tokenizer {
        //public static final int NONE_TOKEN = 1;

        public static final int ID_TOKEN = 2;

        public static final int VALUE_TOKEN = 3;

        public static final int SECTION_TOKEN = 4;

        private static class Token {
            private final int type;

            private final String value;

            public Token(int type, String value) {
                super();
                this.type = type;
                this.value = value;
            }

            public int getType() {
                return type;
            }

            public String getValue() {
                return value;
            }

            @Override
            public String toString() {
                return type + ": " + value;
            }
        }

        private char ch = '\0';

        private final StringBuilder sb = new StringBuilder();

        private Token getToken(Reader reader) throws IOException {
            if (ch == '\0') {
                ch = nextCharacter(reader);
            }
            skipWhitespace(reader);
            if (isLabelCharacter(ch)) {
                sb.setLength(0);
                do {
                    sb.append(ch);
                    ch = nextCharacter(reader);
                } while (isLabelCharacter(ch));
                return new Token(ID_TOKEN, sb.toString());
            } else if (ch == '=') {
                ch = nextCharacter(reader);
                sb.setLength(0);
                if (isValueCharacter(ch)) {
                    do {
                        sb.append(ch);
                        ch = nextCharacter(reader);
                    } while (isValueCharacter(ch));
                }
                return new Token(VALUE_TOKEN, sb.toString().trim());
            } else if (ch == '[') {
                ch = nextCharacter(reader);
                Token id = getToken(reader);
                // check ]
                ch = nextCharacter(reader); // skip ]
                return new Token(SECTION_TOKEN, id.getValue());
            } else if (ch == '#') {
                do {
                    ch = nextCharacter(reader);
                } while (isValueCharacter(ch));
                return getToken(reader);
            }
            return null;
        }

        private boolean isNewLineCharacter(char ch) {
            return NEWLINE.indexOf(ch) != -1;
        }

        private boolean isLabelCharacter(char ch) {
            return !isEndOfStream(ch) && "[]=#".indexOf(ch) == -1;
        }

        private boolean isValueCharacter(char ch) {
            return !isEndOfStream(ch) && !isNewLineCharacter(ch);
        }

        private boolean isEndOfStream(char ch) {
            return (byte) ch == -1;
        }

        private char nextCharacter(Reader reader) throws IOException {
            return (char) reader.read();
        }

        private void skipWhitespace(Reader reader) throws IOException {
            if (Character.isWhitespace(ch)) {
                do {
                    ch = nextCharacter(reader);
                } while (Character.isWhitespace(ch));
            }
        }
    }

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)}");

    private String interpolate(String value) {
        if (value == null || value.indexOf('$') == -1) {
            return value;
        }
        StringBuffer buffer = new StringBuffer();
        Matcher m = VARIABLE_PATTERN.matcher(value);
        while (m.find()) {
            if (m.start() > 0 && value.charAt(m.start() - 1) == '\\') {
                continue;
            }
            String variable = m.group(1);
            String variableValue = variableValues.getProperty(variable);
            if (variableValue != null) {
                m.appendReplacement(buffer, variableValue);
            }
        }
        m.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Set properties that will be the source of variable values in the settings. A variable
     * is of the form ${variable} and will be replaced with values from the
     * map when the setting is retrieved.
     * <p>
     * By default, the System properties are used for variable values. If
     * you use this method, it will override the defaults so use the Properties
     * default value mechanism if you want to chain a custom properties object
     * with System properties as the default.
     *
     * <code><pre>
     * // Custom properties with System properties as default
     * Properties myprops = new Properties(System.getProperties());
     * myprops.load(getPropertiesInputStream());
     * settings.setVariableValues(myprops);
     *
     * // Custom properties with System properties as override
     * Properties myprops = new Properties();
     * myprops.load(getPropertiesInputStream());
     * myprops.putAll(System.getProperties());
     * settings.setVariableValues(myprops);
     * </pre></code>
     *
     * @param variableValues
     * @see Properties
     * @see System
     */
    @Override
    public void setVariableValues(Properties variableValues) {
        this.variableValues = variableValues;
    }

    /**
     * Adds defaults to the settings. Will not delete existing settings not
     * overlapping with the new defaults, but will overwrite existing settings
     * specified in this call.
     *
     * @param defaults
     */
    @Override
    public void set(Map<Object, Object> defaults) {
        getOrCreateSessionProperties(DEFAULT_SESSION_ID).putAll(defaults);
    }

    /**
     * Set a default boolean parameter.
     *
     * @param key   the settings key
     * @param value the settings value
     */
    @Override
    public void setBool(String key, boolean value) {
        setBool(DEFAULT_SESSION_ID, key, value);
    }

    /**
     * Set a default double parameter.
     *
     * @param key   the settings key
     * @param value the settings value
     */
    @Override
    public void setDouble(String key, double value) {
        setDouble(DEFAULT_SESSION_ID, key, value);
    }

    /**
     * Set a default long parameter.
     *
     * @param key   the settings key
     * @param value the settings value
     */
    @Override
    public void setLong(String key, long value) {
        setLong(DEFAULT_SESSION_ID, key, value);
    }

    /**
     * Set a default string parameter.
     *
     * @param key   the settings key
     * @param value the settings value
     */
    @Override
    public void setString(String key, String value) {
        setString(DEFAULT_SESSION_ID, key, value.trim());
    }

    @Override
    public int size() {
        // Always a default section
        return sections.size() - 1;
    }

    @Override
    public Dictionary get(SessionID sessionID) throws ConfigError {
        return new Dictionary(null, getSessionProperties(sessionID));
    }

    @Override
    public void set(SessionID sessionID, Dictionary dictionary) throws ConfigError {
        Properties p = getOrCreateSessionProperties(sessionID);
        p.clear();
        p.putAll(dictionary.toMap());
    }

    @Override
    public Dictionary get() {
        return new Dictionary(null, getDefaultProperties());
    }

    @Override
    public void set(Dictionary dictionary) throws ConfigError {
        getDefaultProperties().putAll(dictionary.toMap());
    }

    @Override
    public void toString(PrintWriter writer) {
        try {
            writeSection("[DEFAULT]", writer, getDefaultProperties());
            Iterator<SessionID> s = sectionIterator();
            while (s.hasNext()) {
                try {
                    writeSection("[SESSION]", writer, getSessionProperties(s.next()));
                } catch (ConfigError e) {
                    log.error("Invalid session", e);
                }
            }
        } finally {
            writer.flush();
        }
    }

    @Override
    public void toStream(OutputStream out) {
        toString(new PrintWriter(new OutputStreamWriter(out)));
    }

    private void writeSection(String sectionName, PrintWriter writer, Properties properties) {
        writer.println(sectionName);
        for (Object o : properties.keySet()) {
            String key = (String) o;
            writer.print(key);
            writer.print("=");
            writer.println(properties.getProperty(key));
        }
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        toString(new PrintWriter(writer));
        return writer.toString();
    }

    public static int[] parseSettingReconnectInterval(String raw) {
        if (raw == null || raw.length() == 0) {
            return null;
        }
        String multiplierCharacter = raw.contains("*") ? "\\*" : "x";
        String[] data = raw.split(";");
        List<Integer> result = new ArrayList<>();
        for (String multi : data) {
            String[] timesSec = multi.split(multiplierCharacter);
            int times;
            int secs;
            try {
                if (timesSec.length > 1) {
                    times = Integer.parseInt(timesSec[0]);
                    secs = Integer.parseInt(timesSec[1]);
                } else {
                    times = 1;
                    secs = Integer.parseInt(timesSec[0]);
                }
            } catch (NumberFormatException e) {
                throw new InvalidParameterException(
                        "Invalid number '"
                                + multi
                                + "' in '"
                                + raw
                                + "'. Expected format: [<multiplier>x]<seconds>;[<multiplier>x]<seconds>;...");
            }
            for (int ii = 0; ii != times; ++ii) {
                result.add(secs);
            }
        }
        int[] ret = new int[result.size()];
        int ii = 0;
        for (Integer sec : result) {
            ret[ii++] = sec;
        }
        return ret;
    }

    public static Set<InetAddress> parseRemoteAddresses(String raw) {
        if (raw == null || raw.length() == 0) {
            return null;
        }
        String[] data = raw.split(",");
        Set<InetAddress> result = new HashSet<>();
        for (String multi : data) {
            try {
                result.add(InetAddress.getByName(multi));
            } catch (UnknownHostException e) {
                log.error("Ignored unknown host : " + multi, e);
            }
        }
        return result;
    }

}
