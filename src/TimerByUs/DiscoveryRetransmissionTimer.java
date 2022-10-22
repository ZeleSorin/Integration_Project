package TimerByUs;


import client.Message;


import java.util.concurrent.BlockingQueue;

public class DiscoveryRetransmissionTimer implements Runnable {

    private Message discoveryMessage;
private BlockingQueue<Message > sending;
    public DiscoveryRetransmissionTimer(Message m, BlockingQueue<Message> sendingQueue) {
        this.discoveryMessage = m;
        this.sending = sendingQueue;
    }


    @Override
    public void run() {
        while (true) {

            try {
                Thread.sleep(25000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                sending.put(discoveryMessage);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}

