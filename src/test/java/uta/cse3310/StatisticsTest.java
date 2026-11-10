package uta.cse3310;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticsTest {

    private Statistics stats;

    @Before
    public void setUp() {
        stats = new Statistics();
    }

    @Test
    public void testInitialRunningTimeZero() {
        assertEquals(Long.valueOf(0), stats.getRunningTime());
    }

    @Test
    public void testSetRunningTime() {
        stats.setRunningTime(120L);
        assertEquals(Long.valueOf(120), stats.getRunningTime());
    }

    @Test
    public void testRunningTimeUpdates() {
        stats.setRunningTime(60L);
        stats.setRunningTime(180L);
        assertEquals(Long.valueOf(180), stats.getRunningTime());
    }

    @Test
    public void testInitialXWinsZero() {
        assertEquals(Integer.valueOf(0), stats.getXWins());
    }

    @Test
    public void testSetXWins() {
        stats.setXWins(5);
        assertEquals(Integer.valueOf(5), stats.getXWins());
    }

    @Test
    public void testInitialOWinsZero() {
        assertEquals(Integer.valueOf(0), stats.getOWins());
    }

    @Test
    public void testSetOWins() {
        stats.setOWins(3);
        assertEquals(Integer.valueOf(3), stats.getOWins());
    }

    @Test
    public void testInitialDrawsZero() {
        assertEquals(Integer.valueOf(0), stats.getDraws());
    }

    @Test
    public void testSetDraws() {
        stats.setDraws(2);
        assertEquals(Integer.valueOf(2), stats.getDraws());
    }

    @Test
    public void testInitialTotalGamesZero() {
        assertEquals(Integer.valueOf(0), stats.getTotalGames());
    }

    @Test
    public void testSetTotalGames() {
        stats.setTotalGames(10);
        assertEquals(Integer.valueOf(10), stats.getTotalGames());
    }

    @Test
    public void testInitialGamesInProgressZero() {
        assertEquals(Integer.valueOf(0), stats.getGamesInProgress());
    }

    @Test
    public void testSetGamesInProgress() {
        stats.setGamesInProgress(4);
        assertEquals(Integer.valueOf(4), stats.getGamesInProgress());
    }

    @Test
    public void testAllFieldsIndependent() {
        stats.setXWins(1);
        stats.setOWins(2);
        stats.setDraws(3);
        stats.setTotalGames(6);
        stats.setGamesInProgress(1);
        stats.setRunningTime(999L);
        assertEquals(Integer.valueOf(1), stats.getXWins());
        assertEquals(Integer.valueOf(2), stats.getOWins());
        assertEquals(Integer.valueOf(3), stats.getDraws());
        assertEquals(Integer.valueOf(6), stats.getTotalGames());
        assertEquals(Integer.valueOf(1), stats.getGamesInProgress());
        assertEquals(Long.valueOf(999), stats.getRunningTime());
    }
}
