package com.newtube.mobile.ui.browse;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** YEWPLAY(subs): o dia de um vídeo de Inscrições a partir do texto relativo do YouTube. */
public class FeedDaysTest {
    @Test
    public void portuguese() {
        assertEquals(FeedDays.TODAY, FeedDays.parse("12 mil visualizações · há 5 horas"));
        assertEquals(FeedDays.TODAY, FeedDays.parse("há 12 min"));
        assertEquals(FeedDays.YESTERDAY, FeedDays.parse("6,7 mil visualizações · há 1 dia"));
        assertEquals(FeedDays.THIS_WEEK, FeedDays.parse("há 3 dias"));
        assertEquals(FeedDays.EARLIER, FeedDays.parse("há 2 semanas"));
        assertEquals(FeedDays.EARLIER, FeedDays.parse("há 5 meses"));
    }

    @Test
    public void english() {
        assertEquals(FeedDays.TODAY, FeedDays.parse("1.2K views · 3 hours ago"));
        assertEquals(FeedDays.YESTERDAY, FeedDays.parse("1 day ago"));
        assertEquals(FeedDays.THIS_WEEK, FeedDays.parse("6 days ago"));
        assertEquals(FeedDays.EARLIER, FeedDays.parse("7 days ago"));
    }

    @Test
    public void unknown() {
        assertEquals(FeedDays.UNKNOWN, FeedDays.parse(null));
        assertEquals(FeedDays.UNKNOWN, FeedDays.parse("Canal Tal"));
    }
}
