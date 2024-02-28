from datetime import datetime, timedelta

def is_daylight_savings_time(dt,timeZone):
    """ Checks if the datetime dt in timezone timeZone is in daylight savings time """
    aware_dt = timeZone.localize(dt)
    return aware_dt.dst() != timedelta(0,0)
