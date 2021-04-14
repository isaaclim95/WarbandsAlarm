import java.awt.*;
import java.io.File;
import java.time.*;
import java.util.*;
import java.util.List;


public class WarbandsAlarm {

    public static void main(String[] args)  {

        long minutesBeforeNotifying = 10L;
        DayOfWeek dayOfWeek = LocalDateTime.now().getDayOfWeek();

        Hashtable<DayOfWeek, List<LocalDateTime>> campTimes = getCampTimes();
        Hashtable<DayOfWeek, List<LocalDateTime>> campBufferTimes = (Hashtable<DayOfWeek, List<LocalDateTime>>) campTimes.clone();
        for(DayOfWeek key : campBufferTimes.keySet()) {
            ArrayList<LocalDateTime> bufferTimes = new ArrayList<>();
            for(LocalDateTime time : campBufferTimes.get(key))  {
                bufferTimes.add(time.minusMinutes(minutesBeforeNotifying));
            }
            campBufferTimes.replace(key, bufferTimes);
        }

        System.out.println(campTimes);
        System.out.println(campBufferTimes);

        runTimeCheckingLoop(dayOfWeek, campTimes, campBufferTimes);
    }

    public static Hashtable<DayOfWeek, List<LocalDateTime>> getCampTimes()   {
        ArrayList<String> lines = new ArrayList<>();
        Hashtable<DayOfWeek, List<LocalDateTime>> campTimes = new Hashtable<>();

        LocalDate curDateTime = LocalDate.now();
        int curYear = curDateTime.getYear();
        Month curMonth = curDateTime.getMonth();
        int curDayOfMonth = curDateTime.getDayOfMonth();

        try {
            File file = new File("src/warbands.txt");
            Scanner reader = new Scanner(file);
            while(reader.hasNextLine()) {
                lines.add(reader.nextLine());
            }
            reader.close();
        } catch (Exception e)   {
            e.printStackTrace();
        }

        for(String line : lines)   {
            ArrayList<LocalDateTime> times = new ArrayList<>();
            List<String> lineData = Arrays.asList(line.split(" "));
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(lineData.get(0));
            System.out.println("Day of week: " + dayOfWeek.name());
            for(String time : lineData.subList(1, lineData.size())) {
                int hour = Integer.parseInt(time.split(":")[0]);
                int minute = Integer.parseInt(time.split(":")[1]);
                System.out.println("Hour: " + hour + " Minute: " + minute);
                times.add(
                        LocalDateTime.of(
                                curYear, curMonth, curDayOfMonth, hour, minute)
                );
            }
            campTimes.put(dayOfWeek, times);
        }
        return campTimes;
    }

    public static void runTimeCheckingLoop(
            DayOfWeek dayOfWeek, Hashtable<DayOfWeek,
            List<LocalDateTime>> campTimes, Hashtable<DayOfWeek,
            List<LocalDateTime>> campBufferTimes)   {

        while(true) {
            boolean notified = false;

            LocalDateTime curDateTime = LocalDateTime.now();
            int curMinute = curDateTime.getMinute();


            for(int i = 0; i < campTimes.get(dayOfWeek).size(); i++)    {
                System.out.println("______");
                System.out.println(curDateTime);
                System.out.println(campTimes.get(dayOfWeek).get(i));
                System.out.println(campBufferTimes.get(dayOfWeek).get(i));
                System.out.println(curDateTime.isBefore(campTimes.get(dayOfWeek).get(i)));
                System.out.println(curDateTime.isAfter(campBufferTimes.get(dayOfWeek).get(i)));
                System.out.println("______");
                if(curDateTime.isBefore(campTimes.get(dayOfWeek).get(i))
                        && curDateTime.isAfter(campBufferTimes.get(dayOfWeek).get(i)))   {
                    notify(campTimes.get(dayOfWeek).get(i).getMinute() - curMinute);
                    notified = true;
                    try {
                        System.out.println("Sleeping for 20 minutes...");
                        Thread.sleep(600000L);
                    } catch (Exception e)   {
                        e.printStackTrace();
                    }
                }
            }
            try {
                System.out.println("Sleeping for 1 minute...");
                Thread.sleep(60000L);
            } catch (Exception e)   {
                e.printStackTrace();
            }
        }

    }

    /**
     *
     * Sends notification message
     */
    public static void notify(int minutesLeft) {
        System.out.println("Notifying warbands is starting");
        if(SystemTray.isSupported())    {
            Image image = Toolkit.getDefaultToolkit().createImage("src/icon.png");
            SystemTray tray = SystemTray.getSystemTray();
            TrayIcon trayIcon = new TrayIcon(image);
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (Exception e)   {
                e.printStackTrace();
            }
            trayIcon.displayMessage(
                    "Warbands Alarm",
                    "Warbands is going to start in " + minutesLeft + "minutes",
                    TrayIcon.MessageType.INFO);
        }
    }

}
