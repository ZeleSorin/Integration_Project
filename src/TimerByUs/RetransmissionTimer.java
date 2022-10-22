package TimerByUs;

import NetworkLayer.NetworkLayer;

import java.util.Date;
import java.util.Map;

public class RetransmissionTimer implements Runnable {

    Map<Integer, Date> myMap;

    public RetransmissionTimer(Map<Integer, Date> map) {
        this.myMap = map;
    }


    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Date currentDate = new Date(System.currentTimeMillis());
            for (Integer k : myMap.keySet()) {
                long timePassed = (currentDate.getTime() - myMap.get(k).getTime()) / 1000 + 1;
                if (timePassed > 25){

                    NetworkLayer.retransmission(k);
                }
            }
        }
    }
}
