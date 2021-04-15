import java.awt.*;
import java.io.File;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * TODO: Only schedule a notification after warbands is not in progress (State 1 - Upcoming, State 2 - In progress)
 * TODO: Make timezone UTC for universal use.
 * TODO: GUI and taskbar icon
 */

public class WarbandsAlarm2 {

    public static void main(String[] args)  {

        int minutesBeforeCampStartNotify = 10;

        runDaemon(minutesBeforeCampStartNotify);

    }


    /**
     * Runs the program monitoring loop
     * @param minutesBeforeCampStartNotify: Number of minutes prior to camp starting to notify user
     */
    public static void runDaemon(int minutesBeforeCampStartNotify)  {

        Hashtable<DayOfWeek, ArrayList<Long>> dayToEpochMillis = processFile("src/warbands.txt");
        scheduleNextNotification(minutesBeforeCampStartNotify, dayToEpochMillis);

    }

    public static void scheduleNextNotification(int minutesBeforeCampStartNotify, Hashtable<DayOfWeek, ArrayList<Long>> dayToEpochMillis) {

        do {
            long nextCampTime = getNextCampTime(dayToEpochMillis);

            AtomicBoolean completed = new AtomicBoolean(false);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    completed.set(true);
                    System.out.println("Inside : " + Thread.currentThread().getName());
                    System.out.println("Warbands is starting now");

                    try {
                        Thread.sleep(60000L);
                    } catch (Exception e)   {
                        e.printStackTrace();
                    }

                }
            };

            long nextCampTimeBuffer = nextCampTime - Integer.toUnsignedLong(minutesBeforeCampStartNotify) * 60 * 1000;
            System.out.println("nextCampTimeBuffer: " + nextCampTimeBuffer);

            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.schedule(runnable,  nextCampTimeBuffer - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            try {
                scheduledExecutorService.awaitTermination(60000L, TimeUnit.MILLISECONDS);
            } catch(Exception e)    {
                e.printStackTrace();
            }

        } while(true);

    }

    /**
     * Processes file [pathname] and returns a Hashtable with days of the week as keys,
     * and the camp times as unix epoch times
     */
    public static Hashtable<DayOfWeek, ArrayList<Long>> processFile(String pathname)   {

        Hashtable<DayOfWeek, List<LocalDateTime>> campTimes = getCampTimes(getLinesFromFile(pathname));

        Hashtable<DayOfWeek, ArrayList<Long>> campTimesInEpochMillis = new Hashtable<>();

        for(DayOfWeek day : campTimes.keySet()) {

            ArrayList<Long> epochMillis = new ArrayList<>();

            for(int j = 0; j < campTimes.get(day).size(); j++)   {

                epochMillis.add(localDateTimeToEpochMillis(campTimes.get(day).get(j)));

            }

            Collections.sort(epochMillis);

            campTimesInEpochMillis.put(day, epochMillis);

        }

        return campTimesInEpochMillis;

    }

    /**
     *
     * Returns the next camp time from the current point in time in epoch milliseconds
     *
     */
    public static long getNextCampTime(Hashtable<DayOfWeek, ArrayList<Long>> dayToEpochMillis)    {

        LocalDateTime curDateTime = LocalDateTime.now();

        long curEpochMillis = localDateTimeToEpochMillis(curDateTime);

        long nextCampTime = 0;
        long minTime = Integer.MAX_VALUE;

        for(DayOfWeek day : dayToEpochMillis.keySet())  {

            for(long campTime : dayToEpochMillis.get(day)) {

                long difference =  campTime - curEpochMillis;

                if (difference >= 0 && difference < minTime) {

                    minTime = difference;
                    nextCampTime = campTime;

                }

            }
        }
        System.out.println("nextCampTime(): " + nextCampTime);
        return nextCampTime;

    }

    public static ArrayList<String> getLinesFromFile(String pathname) {

        ArrayList<String> lines = new ArrayList<>();

        try {

            File file = new File(pathname);
            Scanner reader = new Scanner(file);

            while(reader.hasNextLine()) {

                lines.add(reader.nextLine());

            }

            reader.close();

        } catch (Exception e)   {

            e.printStackTrace();

        }

        return lines;
    }

    public static long localDateTimeToEpochMillis(LocalDateTime ldt)  {

        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());

        return zdt.toInstant().toEpochMilli();

    }


    public static Hashtable<DayOfWeek, List<LocalDateTime>> getCampTimes(ArrayList<String> lines)   {

        Hashtable<DayOfWeek, List<LocalDateTime>> campTimes = new Hashtable<>();

        for(String line : lines)   {

            ArrayList<LocalDateTime> times = new ArrayList<>();
            List<String> lineData = Arrays.asList(line.split(" "));
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(lineData.get(0));

            for(String time : lineData.subList(1, lineData.size())) {

                int hour = Integer.parseInt(time.split(":")[0]);
                int minute = Integer.parseInt(time.split(":")[1]);
                LocalDateTime campTime = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(dayOfWeek)).withHour(hour).withMinute(minute).withSecond(0);
                times.add(campTime);

            }

            campTimes.put(dayOfWeek, times);

        }

        return campTimes;

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
                trayIcon.displayMessage(
                        "Warbands Alarm",
                        "Warbands is going to start in " + minutesLeft + "minutes",
                        TrayIcon.MessageType.INFO);

            } catch (Exception e)   {

                e.printStackTrace();

            }
        }
    }
}
