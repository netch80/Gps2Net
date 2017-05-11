package ua.kiev.netch.gps2net;

import android.location.Criteria;

/**
 * Created by netch on 5/7/17.
 */

public class LocationUtils {
    static Criteria genLocationCriteria() {
        Criteria criteria = new Criteria();
        // common requirements
        criteria.setBearingAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);
        criteria.setSpeedAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setSpeedRequired(false);
        criteria.setVerticalAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setAltitudeRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_LOW);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }
}
