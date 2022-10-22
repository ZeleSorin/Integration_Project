package TimerByUs;

import NetworkLayer.NetworkLayer;

import java.util.Date;
import java.util.Map;

public class TimerByUs extends Thread {
    Date timeCurrentTime;
    Map<Integer, Date> timeMap;

    public TimerByUs(Map<Integer, Date> map) {
        timeCurrentTime = new Date(System.currentTimeMillis());
        timeMap = map;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Date curentDate = new Date(System.currentTimeMillis());
            for (Integer k : timeMap.keySet()) {

                long timePassed = (curentDate.getTime() - timeMap.get(k).getTime()) / 1000 + 1;
                if (timePassed > 50) {
                    timeMap.remove(k);
                    System.out.println("REMOVED USER " + k);
                }

            }
        }
    }

    /**
     * Method returns the difference in seconds .
     * @param timeOne starting time.
     * @param timeTwo end time.
     * @return difference between timeTwo and timeOne
     */
    public long timeDifference(Date timeOne, Date timeTwo){
        long timeDiff= timeTwo.getTime() - timeOne.getTime();
        return timeDiff/1000+1;

    }
}
