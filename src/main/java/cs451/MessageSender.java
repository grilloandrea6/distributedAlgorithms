package cs451;
import java.util.TimerTask;


public class MessageSender {

    class myMessageSender extends TimerTask {
        myMessageSender(AckKeeper ackKeeper) {
            super();

        }
        @Override
        public void run() {
            System.out.println("Sending message");
            // send message
        }
    }

}
