import java.awt.*;
import java.io.File;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import java.awt.*;


/**
 * TODO: Only schedule a notification after warbands is not in progress (State 1 - Upcoming, State 2 - In progress)
 * TODO: Make timezone UTC for universal use.
 * TODO: GUI and taskbar icon
 */



public class WarbandsAlarm {

    enum CampStatus {
        INACTIVE,
        STARTING,
        STARTED
    }

    public static final int MINUTES_BEFORE_CAMP_START_NOTIFY = 10;

    private final Hashtable<DayOfWeek, ArrayList<Long>> dayToEpochMillis;

    // Constructor - Sets everything up
    WarbandsAlarm()    {

        dayToEpochMillis = processFile("src/warbands.txt");

    }



    // Returns the current CampStatus
    public CampStatus getCurrentCampStatus()    {
        long nextOrCurrentCampTime = getNextOrCurrentCampTime();
        long currentEpochTimeMillis = getCurrentEpochTimeMillis();

        // It's in the future
        if(nextOrCurrentCampTime - currentEpochTimeMillis > 0)  {
            // Check if less than 10 minutes till spawn - CampStatus.STARTING
            if(nextOrCurrentCampTime - currentEpochTimeMillis < MINUTES_BEFORE_CAMP_START_NOTIFY * 60 * 1000)   {
                System.out.println("CampStatus.STARTING");
                return CampStatus.STARTING;
            // Camp spawns more than 10 minutes away - CampStatus.STARTING
            } else  {
                System.out.println(nextOrCurrentCampTime - currentEpochTimeMillis);
                System.out.println("CampStatus.INACTIVE1");
                return CampStatus.INACTIVE;
            }
        // It's in the past
        } else  {
            // Camp has started less than 10 minutes ago
            if(currentEpochTimeMillis - nextOrCurrentCampTime < MINUTES_BEFORE_CAMP_START_NOTIFY * 60 * 1000)   {
                System.out.println("CampStatus.STARTED");
                return CampStatus.STARTED;
            }
        }
        // Camp spawns in more than 10 minutes
        System.out.println("CampStatus.INACTIVE2");
        return CampStatus.INACTIVE;
    }

    // Gives time in milliseconds till next camp spawn
    public long getTimeTillNextCamp()   {
        return getNextOrCurrentCampTime() - getCurrentEpochTimeMillis();
    }

    public long getNextOrCurrentCampTime()    {

        long curEpochMillis = getCurrentEpochTimeMillis();

        long nextOrCurrentCampTime = 0;
        long minTime = Integer.MAX_VALUE;

        for(DayOfWeek day : dayToEpochMillis.keySet())  {

            for(long campTime : dayToEpochMillis.get(day)) {

                long difference =  campTime - curEpochMillis;

                // Camp is in the future
                if(difference > 0) {
                    if(difference < minTime)    {
                        minTime = difference;
                        nextOrCurrentCampTime = campTime;
                    }
                } else  {
                    // Camp is current
                    if(difference * -1 < MINUTES_BEFORE_CAMP_START_NOTIFY * 60 * 1000)  {
                        minTime = difference;
                        nextOrCurrentCampTime = campTime;
                    }
                }

                }

            }
            System.out.println("getNextOrCurrentCampTime(): " + Instant.ofEpochMilli(nextOrCurrentCampTime).atZone(ZoneId.systemDefault()).toLocalDateTime());
            return nextOrCurrentCampTime;
        }




    /////////////////////////////////AUX FUNCTIONS///////////////////////////////////////////
    // Auxiliary function which returns the current time in epoch milliseconds
    public long getCurrentEpochTimeMillis()   {
        return localDateTimeToEpochMillis(LocalDateTime.now());
    }


    /**
     * Runs the program monitoring loop
     */
    public void runDaemon()  {

        Hashtable<DayOfWeek, ArrayList<Long>> dayToEpochMillis = processFile("src/warbands.txt");
        scheduleNextNotification(MINUTES_BEFORE_CAMP_START_NOTIFY, dayToEpochMillis);

    }

    public void scheduleNextNotification(int minutesBeforeCampStartNotify, Hashtable<DayOfWeek, ArrayList<Long>> dayToEpochMillis) {

        do {
            long nextCampTime = getNextCampTime2(dayToEpochMillis, minutesBeforeCampStartNotify);
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    System.out.println("Inside : " + Thread.currentThread().getName());
                    System.out.println("Warbands is starting now");
                    WarbandsAlarm.this.notify(10);

                }


            };

            long nextCampTimeBuffer = nextCampTime - Integer.toUnsignedLong(minutesBeforeCampStartNotify) * 60 * 1000;
            System.out.println(
                    "Current time: " + LocalDateTime.now() +
                            "\t nextCampTimeBuffer: " +
                            Instant.ofEpochMilli(nextCampTimeBuffer).atZone(ZoneId.systemDefault()).toLocalDateTime());


            scheduledExecutorService.schedule(runnable,  nextCampTimeBuffer - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            try {
                // 15 minutes timeout
                scheduledExecutorService.shutdown();
                scheduledExecutorService.awaitTermination(27000000, TimeUnit.MILLISECONDS);
                System.out.println("Tasks finished");
            } catch(Exception e)    {
                e.printStackTrace();
            }

        } while(true);

    }

    /**
     * Processes file [pathname] and returns a Hashtable with days of the week as keys,
     * and the camp times as unix epoch times
     */
    public Hashtable<DayOfWeek, ArrayList<Long>> processFile(String pathname)   {

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
     * Must be at least [timeBuffer] minutes before the start of the camp
     *
     */
    public long getNextCampTime2(Hashtable<DayOfWeek, ArrayList<Long>> dayToEpochMillis, int timeBuffer)    {

        LocalDateTime curDateTime = LocalDateTime.now();

        long curEpochMillis = localDateTimeToEpochMillis(curDateTime);

        long nextCampTime = 0;
        long minTime = Integer.MAX_VALUE;

        for(DayOfWeek day : dayToEpochMillis.keySet())  {

            for(long campTime : dayToEpochMillis.get(day)) {

                long difference =  campTime - curEpochMillis;

                if (difference >= 0 && difference < minTime && (difference > (long) timeBuffer * 60 * 1000) ) {

                    minTime = difference;
                    nextCampTime = campTime;

                }

            }
        }
        System.out.println("nextCampTime(): " + Instant.ofEpochMilli(nextCampTime).atZone(ZoneId.systemDefault()).toLocalDateTime());
        return nextCampTime;

    }

    public ArrayList<String> getLinesFromFile(String pathname) {

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

    public long localDateTimeToEpochMillis(LocalDateTime ldt)  {

        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());

        return zdt.toInstant().toEpochMilli();

    }


    public Hashtable<DayOfWeek, List<LocalDateTime>> getCampTimes(ArrayList<String> lines)   {
        Hashtable<DayOfWeek, List<LocalDateTime>> campTimes = new Hashtable<>();
        try {

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
        } catch (Exception e)   {
            System.out.println("File is invalid somehow");
            e.printStackTrace();
            System.exit(1);
        }


        return campTimes;


    }

    /**
     *
     * Sends notification message
     */
    public void notify(int minutesLeft) {

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
