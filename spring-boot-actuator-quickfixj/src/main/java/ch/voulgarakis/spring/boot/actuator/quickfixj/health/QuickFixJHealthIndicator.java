package ch.voulgarakis.spring.boot.actuator.quickfixj.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import quickfix.Connector;
import quickfix.Session;
import quickfix.mina.acceptor.AbstractSocketAcceptor;
import quickfix.mina.initiator.AbstractSocketInitiator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuickFixJHealthIndicator implements HealthIndicator {

    private static final Status AMBER = new Status("AMBER", "Restricted functionality");

    private final Connector connector;

    public QuickFixJHealthIndicator(Connector connector) {
        this.connector = connector;
    }

    private static Health getInitiatorSessionHealth(Session session) {
        boolean enabled = session.isEnabled();
        boolean loggedOn = session.isLoggedOn();
        boolean wasKickedOff = !session.sentLogout() && session.receivedLogout();
        boolean loggedOut = session.sentLogout() && session.receivedLogout();
        String sessionId = session.getSessionID().toString();
        if (enabled) {
            if (loggedOn) {
                return Health.up().withDetail(sessionId, "Logged On").build();
            } else if (wasKickedOff) {
                return Health.down().withDetail(sessionId, "FIX Server Logged Out").build();
            } else if (loggedOut) {
                return Health.outOfService().withDetail(sessionId, "Logged Out").build();
            } else {
                return Health.unknown().withDetail(sessionId, "Status Unknown for Session: " + session).build();
            }
        } else {
            return Health.outOfService().withDetail(sessionId, "Disabled").build();
        }
    }

    private static Health getAcceptorSessionHealth(Session session) {
        boolean enabled = session.isEnabled();
        boolean loggedOn = session.isLoggedOn();
        boolean wasKickedOff = !session.sentLogout() && session.receivedLogout();
        boolean loggedOut = session.sentLogout() && session.receivedLogout();
        String sessionId = session.getSessionID().toString();
        if (enabled) {
            if (loggedOn) {
                return Health.up().withDetail(sessionId, "Logged On").build();
            } else if (wasKickedOff) {
                return Health.down().withDetail(sessionId, "FIX Client Logged Out").build();
            } else if (loggedOut) {
                return Health.outOfService().withDetail(sessionId, "Logged Out").build();
            } else {
                return Health.unknown().withDetail(sessionId, "Status Unknown for Session: " + session).build();
            }
        } else {
            return Health.up().withDetail(sessionId, "Disabled").build();
        }
    }

    private static Health combine(List<Health> healths) {
        if (healths.isEmpty()) {
            return Health.outOfService().withDetail("sessions", "None defined").build();
        } else {
            Map<String, Object> details = healths.stream().flatMap(
                    health -> health.getDetails().entrySet().stream()).collect(
                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (healths.stream().allMatch(health -> health.getStatus().equals(Status.UP))) {
                return Health.up().withDetails(details).build();
            } else if (healths.stream().anyMatch(health -> health.getStatus().equals(Status.UP))) {
                return Health.status(AMBER).withDetails(details).build();
            } else {
                return Health.down().withDetails(details).build();
            }
        }
    }

    @Override
    public Health health() {
        if (connector instanceof AbstractSocketInitiator) {
            //All logged on -> UP
            //Some logged on -> AMBER
            //None logged on -> DOWN
            AbstractSocketInitiator socketInitiator = (AbstractSocketInitiator) connector;
            List<Health> healths = socketInitiator.getManagedSessions().stream().
                    map(QuickFixJHealthIndicator::getInitiatorSessionHealth).collect(Collectors.toList());
            return combine(healths);
        } else if (connector instanceof AbstractSocketAcceptor) {
            AbstractSocketAcceptor socketAcceptor = (AbstractSocketAcceptor) connector;
            List<Health> healths = socketAcceptor.getManagedSessions().stream().
                    map(QuickFixJHealthIndicator::getAcceptorSessionHealth).collect(Collectors.toList());
            return combine(healths);
        } else {
            return Health.down().withDetail("connectorType", connector.getClass().getName()).build();
        }
    }
}
