package com.coolweather.android;



import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.Country;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTYR=3;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;

    private List<String> dataList=new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<Country> countryList;

    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=view.findViewById(R.id.title_text);
        backButton=view.findViewById(R.id.back_button);
        listView=view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(view.getContext(),R.layout.simple_list_item1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE)
                {
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }
                else if(currentLevel==LEVEL_CITY)
                {
                    selectedCity=cityList.get(position);
                    queryCountries();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTYR)
                {
                    queryCities();
                }
                else if(currentLevel==LEVEL_CITY)
                {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

/**
 * 查询全国所有省，优先查询数据库，如果没查询到在到服务器查询
 */

private void queryProvinces()
{
    titleText.setText("中国");
    backButton.setVisibility(View.GONE);
    provinceList= DataSupport.findAll(Province.class);
    if(provinceList.size()>0)
    {
        dataList.clear();
        for(Province province:provinceList)
        {
            dataList.add(province.getProvinceName());
            Log.i("tag",province.getProvinceName());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel=LEVEL_PROVINCE;
    }
    else {
        String address="http://guolin.tech/api/china";
        queryFromService(address,"province");
    }
}

/**
 * 查询选中省内说有的市，优先查询数据库，如果没有查询到在到服务器查询
 */
private void queryCities()
{
    titleText.setText(selectedProvince.getProvinceName());
    backButton.setVisibility(View.VISIBLE);
    cityList=DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getProvinceCode())).find(City.class);
    if(cityList.size()>0)
    {
        dataList.clear();
        for(City city:cityList)
        {
            dataList.add(city.getCityName());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel=LEVEL_CITY;
    }
    else
    {
        int provinceCode=selectedProvince.getProvinceCode();
        String address="http://guolin.tech/api/china/"+provinceCode;
        queryFromService(address,"city");
    }
}


/**
 * 查询选中市内所有的县，优先从数据库查询，如果没有便去服务器上查询
 */
private void queryCountries()
{
    titleText.setText(selectedCity.getCityName());
    backButton.setVisibility(View.VISIBLE);
    countryList=DataSupport.where("cityid=?",String.valueOf(selectedCity.getCityCode())).find(Country.class);
    if(cityList.size()>0)
    {
        dataList.clear();
        for(Country country:countryList)
        {
            dataList.add(country.getCountryName());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel=LEVEL_COUNTYR;
    }
    else
    {
        int provinceCode=selectedProvince.getProvinceCode();
        int cityCode=selectedCity.getCityCode();
        String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
        queryFromService(address,"country");
    }
}

    /**
     * 根据传入的地址和类型从服务器中查询省市县的数据
     */
private void queryFromService(String address,final String type)
{
    showProgressDialog();
    HttpUtil.sendOKHttpRequest(address, new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            getActivity().runOnUiThread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void run() {
                    closeProgressDialog();
                    Toast.makeText(getContext(),"加载失败...",Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String responseText=response.body().string();
            boolean result=false;
            if("province".equals(type))
            {
                result= Utility.handleProvinceResponse(responseText);
            }
            else if("city".equals(type))
            {
                result=Utility.handlerCityResponse(responseText,selectedProvince.getId());
            }
            else if("country".equals(type))
            {
                result=Utility.handlerCountryResponse(responseText,selectedCity.getId());
            }
            if(result)
            {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        if("province".equals(type))
                        {
                            queryProvinces();
                        }
                        else if("city".equals(type))
                        {
                            queryCities();
                        }
                        else if("country".equals(type))
                        {
                            queryCountries();
                        }
                    }
                });
            }
        }
    });
}

    /**
 * 显示进度对话框
 */
private void showProgressDialog()
{
    if(progressDialog==null)
    {progressDialog=new ProgressDialog(getContext());
    }
    progressDialog.setMessage("正在加载...");
    progressDialog.setCanceledOnTouchOutside(false);
}


    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog()
    {
        if(progressDialog!=null)
        {
            progressDialog.dismiss();
            progressDialog.cancel();
    }
    }
}
