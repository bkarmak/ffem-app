package org.akvo.caddisfly.sensor.colorimetry.strip.util.detector;

import org.akvo.caddisfly.sensor.colorimetry.strip.util.Constant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by linda on 9/13/15
 */
public class FinderPatternInfoToJson {

    public static String toJson(FinderPatternInfo info) {
        JSONObject jsonObject = new JSONObject();

        try {
            JSONArray topLeft = new JSONArray();
            topLeft.put(info.getTopLeft().getX());
            topLeft.put(info.getTopLeft().getY());
            jsonObject.put(Constant.TOPLEFT, topLeft);

            JSONArray topRight = new JSONArray();
            topRight.put(info.getTopRight().getX());
            topRight.put(info.getTopRight().getY());
            jsonObject.put(Constant.TOPRIGHT, topRight);

            JSONArray bottomLeft = new JSONArray();
            bottomLeft.put(info.getBottomLeft().getX());
            bottomLeft.put(info.getBottomLeft().getY());
            jsonObject.put(Constant.BOTTOMLEFT, bottomLeft);

            JSONArray bottomRight = new JSONArray();
            bottomRight.put(info.getBottomRight().getX());
            bottomRight.put(info.getBottomRight().getY());
            jsonObject.put(Constant.BOTTOMRIGHT, bottomRight);

            return jsonObject.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


}
