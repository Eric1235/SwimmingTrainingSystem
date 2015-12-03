﻿package com.scnu.swimmingtrainingsystem.activity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.litepal.LitePalApplication;

import com.scnu.swimmingtrainingsystem.util.Constants;

import android.app.Activity;

public class MyApplication extends LitePalApplication {

	private Map<String, Object> mMap;
	private List<Activity> mList = new LinkedList<Activity>();

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		// 初始化全局变量

		mMap = new HashMap<String, Object>();

		// 保存是第几次计时，提醒用户是第几次计时之中
		mMap.put(Constants.CURRENT_SWIM_TIME, 0);
		// 保存所选的计划ID
		mMap.put(Constants.PLAN_ID, 0);
		// 保存开始计时的日期
		mMap.put(Constants.TEST_DATE, "");
		// 保存手动匹配计时按名次排行的运动员名字,方便除第一趟计时外不用再次拖动运动员进行排行
		mMap.put(Constants.DRAG_NAME_LIST, null);
		// 保存当前登录的用户id
		mMap.put(Constants.CURRENT_USER_ID, "");
		// 保存打开登录页面时检查是否可以连接服务器的状态
		mMap.put(Constants.IS_CONNECT_SERVER, true);
		//保存匹配完成的成绩（趟数）
		mMap.put(Constants.COMPLETE_NUMBER, 0);
		//保存每次计时时的间隔距离
		mMap.put(Constants.INTERVAL, "");
		
	}

	/**
	 * 获取保存必须数据的map
	 * 
	 * @return
	 */
	public Map<String, Object> getMap() {
		return mMap;
	}

	public void setMap(Map<String, Object> map) {
		this.mMap = map;
	}

	// add Activity
	public void addActivity(Activity activity) {
		mList.add(activity);
	}

	/**
	 * 关闭所有窗体，退出本应用
	 */
	public void exit() {
		try {
			for (Activity activity : mList) {
				if (activity != null)
					activity.finish();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	public void onLowMemory() {
		super.onLowMemory();
		System.gc();
	}

	@Override
	public void onTerminate() {
		// TODO Auto-generated method stub
		super.onTerminate();
		System.gc();
	}
}
