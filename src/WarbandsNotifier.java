import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class WarbandsNotifier {
    public static void main(String[] args) {

        WarbandsAlarm wba = new WarbandsAlarm();

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                try {
                    wba.runDaemon();
                } catch (Exception e)   {
                    e.printStackTrace();
                }

            }
        };

        Thread thread1 = new Thread(runnable1);
        thread1.start();

        JFrame frame = new JFrame();//creating instance of JFrame



        // Components
        JLabel timeLabel = new JLabel("test");
        timeLabel.setBounds(0, 0, 900, 300);
        timeLabel.setFont(new Font("Serif", Font.BOLD, 36));
        timeLabel.setForeground(Color.white);

        // Adding components to frame
        frame.add(timeLabel);

        // Setting frame options
        frame.setSize(1000,500);
        frame.setLayout(null); //using no layout managers
        frame.setVisible(true); //making the frame visible
        frame.setResizable(false);
        frame.getContentPane().setBackground(Color.DARK_GRAY);

        Runnable constantlyUpdateTime = new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while(true) {
                    try {

                        WarbandsAlarm.CampStatus campStatus = wba.getCurrentCampStatus();
                        long timeTillNextCamp = wba.getTimeTillNextCamp();

                        if(campStatus == WarbandsAlarm.CampStatus.INACTIVE || campStatus == WarbandsAlarm.CampStatus.STARTING)   {
                            timeLabel.setText("Warbands starts in " + String.format("%02d:%02d:%02d",
                                    TimeUnit.MILLISECONDS.toHours(timeTillNextCamp),
                                    TimeUnit.MILLISECONDS.toMinutes(timeTillNextCamp) -
                                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeTillNextCamp)), // The change is in this line
                                    TimeUnit.MILLISECONDS.toSeconds(timeTillNextCamp) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeTillNextCamp))));
                        } else if(campStatus == WarbandsAlarm.CampStatus.STARTED)   {
                            timeLabel.setText("Warbands has started!");
                        }

                        Thread.sleep(1000L);
                    } catch (Exception e)   {
                        e.printStackTrace();
                    }
                }

            }
        };


        try {
            System.out.println("Starting Thread...");
            Thread thread2 = new Thread(constantlyUpdateTime);
            thread2.start();
            thread2.join();
        } catch (Exception e)   {
            e.printStackTrace();
        }





    }
}  