import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogging {
    private static final Logger LOG = LoggerFactory.getLogger(TestLogging.class);
    
    public static void main(String[] args) {
        System.out.println("User home: " + System.getProperty("user.home"));
        LOG.info("This is an INFO message");
        LOG.warn("This is a WARN message");
        LOG.error("This is an ERROR message");
        System.out.println("Logging test completed");
    }
}
