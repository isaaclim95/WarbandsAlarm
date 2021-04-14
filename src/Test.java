import java.awt.*;
import java.io.File;
import java.time.*;
import java.util.*;
import java.util.List;

public class Test {

    public static void main(String[] args)  {
        LocalDateTime curDateTime = LocalDateTime.now();
        int curYear = curDateTime.getYear();
        Month curMonth = curDateTime.getMonth();
        int curDayOfMonth = curDateTime.getDayOfMonth();


        LocalDateTime dateTime = LocalDateTime.of(
                curYear, curMonth, curDayOfMonth, 0, 10);
        System.out.print(dateTime);
    }
}
