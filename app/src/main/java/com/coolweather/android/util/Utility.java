package com.coolweather.android.util;

import android.text.TextUtils;

import com.coolweather.android.db.City;
import com.coolweather.android.db.Country;
import com.coolweather.android.db.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response)
    {
        if(!TextUtils.isEmpty(response))
        {
            try {
                JSONArray allProvince=new JSONArray(response);
                for(int i=0;i<allProvince.length();i++)
                {
                    JSONObject provinceObj=allProvince.getJSONObject(i);
                    Province province=new Province();
                    province.setProvinceName(provinceObj.getString("name"));
                    province.setProvinceCode(provinceObj.getInt("id"));
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handlerCityResponse(String response,int provinceId)
    {
        if(!TextUtils.isEmpty(response))
        {
            try {
                JSONArray allCity=new JSONArray(response);
                for(int i=0;i<allCity.length();i++)
                {
                    JSONObject cityObj=allCity.getJSONObject(i);
                    City city=new City();
                    city.setCityName(cityObj.getString("name"));
                    city.setCityCode(cityObj.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return  true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handlerCountryResponse(String response,int cityId)
    {
        if(!TextUtils.isEmpty(response))
        {
            try {
                JSONArray allCountry=new JSONArray(response);
                for(int i=0;i<allCountry.length();i++)
                {
                    JSONObject countryObj=allCountry.getJSONObject(i);
                    Country country=new Country();
                    country.setCountryName(countryObj.getString("name"));
                    country.setWeatherId(countryObj.getInt("weather_id"));
                    country.setCityId(cityId);
                    country.save();
                }
                return  true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return  false;
    }
}
