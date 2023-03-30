import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;

public class ScratchTests {
    @Test
    public void numberFormat() {
        DecimalFormat fmt = new DecimalFormat("+00.00;-00.00");
        System.out.println(fmt.format(9.432));
        System.out.println(fmt.format(29.432));
        System.out.println(fmt.format(229.432));
        System.out.println(fmt.format(-9.432));
        System.out.println(fmt.format(-29.432));
        System.out.println(fmt.format(-229.432));

    }
}