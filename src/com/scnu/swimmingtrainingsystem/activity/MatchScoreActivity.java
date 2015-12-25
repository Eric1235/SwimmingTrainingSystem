package com.scnu.swimmingtrainingsystem.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.mobeta.android.dslv.DragSortListView;
import com.scnu.swimmingtrainingsystem.R;
import com.scnu.swimmingtrainingsystem.adapter.DragAdapter;
import com.scnu.swimmingtrainingsystem.adapter.ScoreListAdapter;
import com.scnu.swimmingtrainingsystem.db.DBManager;
import com.scnu.swimmingtrainingsystem.http.JsonTools;
import com.scnu.swimmingtrainingsystem.model.Athlete;
import com.scnu.swimmingtrainingsystem.model.Plan;
import com.scnu.swimmingtrainingsystem.model.Score;
import com.scnu.swimmingtrainingsystem.model.SmallPlan;
import com.scnu.swimmingtrainingsystem.model.SmallScore;
import com.scnu.swimmingtrainingsystem.model.User;
import com.scnu.swimmingtrainingsystem.util.AppController;
import com.scnu.swimmingtrainingsystem.util.CommonUtils;
import com.scnu.swimmingtrainingsystem.util.Constants;
import com.scnu.swimmingtrainingsystem.util.SpUtil;
import com.scnu.swimmingtrainingsystem.util.VolleyUtil;
import com.scnu.swimmingtrainingsystem.view.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchScoreActivity extends Activity implements
		OnItemLongClickListener {

	private MyApplication app;

//	/**
//	 * 回调监听器
//	 */
//	private VolleyUtil.ResponseListener reponseListener;

	private ArrayList<String> scores = new ArrayList<String>();
	/**
	 * 拖拽运动员的适配器
	 */
	private DragAdapter dragAdapter;
	private List<ListView> viewList;
	private List<String> dragDatas1;
	private List<Athlete> dragDatas;
	private boolean isConnected;
	private int userId;
	private Plan plan;
	private ArrayList<String> originScores = new ArrayList<String>();
	//	private ArrayList<String> originNames = new ArrayList<String>();
	private ArrayList<Athlete> originNames = new ArrayList<Athlete>();

	private List<Integer> dragAthleteIDs;

	private AutoCompleteTextView acTextView;
	private DBManager mDbManager;

	private Button btNextTiming, btStatistics;
	private DragSortListView scoreListView;
	private DragSortListView nameListView;
	private ScoreListAdapter adapter;

	private LoadingDialog loadingDialog;
//	private RequestQueue mQueue;
	private Toast mToast;
	private LinearLayout mLayout;
	private RelativeLayout mLayout2;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_matchscore);
		init();
//		try {
//			init();
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//			startActivity(new Intent(this, LoginActivity.class));
//		}
	}

	@SuppressWarnings("unchecked")
	private void init() {
		// TODO Auto-generated method stub
		app = (MyApplication) getApplication();
		app.addActivity(this);
		mDbManager = DBManager.getInstance();
		isConnected = (Boolean) app.getMap().get(Constants.IS_CONNECT_SERVER);
//		mQueue = Volley.newRequestQueue(this);
		/**
		 * 接收传过来的成绩，数组
		 */
		Intent result = getIntent();
		scores = result.getStringArrayListExtra("SCORES");
		originScores.addAll(scores);

		userId = SpUtil.getUID(MatchScoreActivity.this);
		Long planId = (Long) app.getMap().get(Constants.PLAN_ID);
		plan = DataSupport.find(Plan.class, planId);

		mLayout = (LinearLayout) findViewById(R.id.ll_pop);
		mLayout2 = (RelativeLayout) findViewById(R.id.match_score_headbar);
		btNextTiming = (Button) findViewById(R.id.match_done);
		btStatistics = (Button) findViewById(R.id.match_statistic);
		scoreListView = (DragSortListView) findViewById(R.id.matchscore_list);
		nameListView = (DragSortListView) findViewById(R.id.matchName_list);
		acTextView = (AutoCompleteTextView) findViewById(R.id.match_act_current_distance);

		nameListView.setDropListener(onDrop);
		/**
		 * 设置运动员名字不能删除
		 */
//		nameListView.setRemoveListener(onRemove);
		nameListView.setDragScrollProfile(ssProfile);

		scoreListView.setRemoveListener(onRemove2);

		// 设置数据源
		String[] autoStrings = getResources().getStringArray(R.array.swim_length);

		ArrayAdapter<String> tipsAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, autoStrings);
		// 设置AutoCompleteTextView的Adapter
		acTextView.setAdapter(tipsAdapter);
		acTextView.setDropDownHeight(350);
		acTextView.setThreshold(1);

		int numberth = (Integer) app.getMap().get(Constants.CURRENT_SWIM_TIME);
		String intervalString = (String) app.getMap().get(Constants.INTERVAL);
		int intervalDistance = Integer
				.parseInt(intervalString.replace("米", ""));
		acTextView.setText(intervalDistance * numberth + "");
		int totalDistance = plan.getDistance();
		if (totalDistance <= intervalDistance * numberth) {
			btNextTiming.setVisibility(View.GONE);
			btStatistics.setText(getString(R.string.adjust_finish_goto_statistics));
		}

		/**
		 * 获取选择的运动员id
		 */
		dragAthleteIDs = (List<Integer>) app.getMap().get(Constants.DRAG_NAME_LIST_IDS);
//		dragDatas = (List<String>) app.getMap().get(Constants.DRAG_NAME_LIST);
		/**
		 * 通过数据库查询得到运动员
		 */
		dragDatas = DBManager.getInstance().getAthleteByIDs(dragAthleteIDs);
		/**
		 * 备份数据
		 */
		originNames.addAll(dragDatas);
		viewList = new ArrayList<ListView>();
		viewList.add(scoreListView);
		viewList.add(nameListView);
		MyScrollListener mListener = new MyScrollListener();
		/**
		 * 设置成绩不能上下移动
		 */
//		scoreListView.setOnScrollListener(mListener);
		nameListView.setOnScrollListener(mListener);
		adapter = new ScoreListAdapter(scoreListView, this, scores);

		/**
		 * 运动员名单适配器
		 */
		dragAdapter = new DragAdapter(this, R.layout.drag_list_item, dragDatas);
		scoreListView.setAdapter(adapter);
		nameListView.setAdapter(dragAdapter);
		scoreListView.setOnItemLongClickListener(this);
	}

	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			if(from != to){
				Athlete item = (Athlete)dragAdapter.getItem(from);

				dragAdapter.notifyDataSetChanged();
				dragAdapter.remove(item);
				dragAdapter.insert(item, to);
			}

		}
	};

	/**
	 * 运动员名字删除监听器
	 */
	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
		@Override
		public void remove(int which) {
			if (dragDatas.size() > 1) {
				dragAdapter.remove(dragAdapter.getItem(which));
			} else {
				CommonUtils.showToast(MatchScoreActivity.this, mToast,
						getString(R.string.leave_at_least_one_athlete));
			}
			dragAdapter.notifyDataSetChanged();
		}
	};

	private DragSortListView.DragScrollProfile ssProfile = new DragSortListView.DragScrollProfile() {
		@Override
		public float getSpeed(float w, long t) {
			if (w > 0.8f) {
				// Traverse all views in a millisecond
				return ((float) dragAdapter.getCount()) / 0.001f;
			} else {
				return 10.0f * w;
			}
		}
	};

	/**
	 * 成绩删除监听器
	 */
	private DragSortListView.RemoveListener onRemove2 = new DragSortListView.RemoveListener() {
		@Override
		public void remove(int which) {
			if (scores.size() > 1) {
				scores.remove(which);
			} else {
				CommonUtils.showToast(MatchScoreActivity.this, mToast,
						getString(R.string.leave_at_least_one_score));
			}
			adapter.notifyDataSetChanged();
		}
	};

	/**
	 * 匹配完毕，可以进入下一趟计时或者进入本轮总计
	 * 
	 * @param v
	 */
	public void matchDone(View v) {
		int nowCurrent = (Integer) app.getMap()
				.get(Constants.CURRENT_SWIM_TIME);
		String actv = acTextView.getText().toString().trim();
		int crrentDistance = 0;
		if (!TextUtils.isEmpty(actv)) {
			crrentDistance = Integer.parseInt(acTextView.getText().toString()
					.trim().replace("米", ""));
		}
		// 暂时保存到SharePreferences
		String scoresString = JsonTools.creatJsonString(scores);
		String athleteJson = JsonTools.creatJsonString(dragDatas);
		String athleteidJson = JsonTools.creatJsonString(CommonUtils.getAthleteIdsByAthletes(dragAdapter.getAthletes()));
		createDialog(this, nowCurrent, crrentDistance, scoresString,
				athleteJson,athleteidJson);
	}

	/**
	 * 如果当前成绩数目与运动员人数相等,并且是该轮游泳只有一趟则直接保存到数据库
	 * 
	 * @param date
	 * @param nowCurrent
	 * @param distance
	 */
	private void matchSuccess(String date, int nowCurrent, int distance) {
		User user = mDbManager.getUserByUid(userId);
//		List<Athlete> athletes = mDbManager.getAthleteByNames(dragDatas);
		List<Athlete> athletes = dragDatas;
		for (int i = 0; i < scores.size(); i++) {
			Athlete a = athletes.get(i);
			Score s = new Score();
			s.setDate(date);
			s.setTimes(nowCurrent);
			s.setScore(scores.get(i));
			s.setType(Constants.NORMALSCORE);
			s.setAthlete(a);
			s.setDistance(distance);
			s.setP(plan);
			s.setUser(user);
			s.save();
		}
//		isConnected = NetworkUtil.isConnected(this);
		if (isConnected) {
			if (loadingDialog == null) {
				loadingDialog = LoadingDialog.createDialog(this);
				loadingDialog.setMessage(getString(R.string.onSubmitting));
				loadingDialog.setCanceledOnTouchOutside(false);
			}
			loadingDialog.show();
			addScoreRequest(date);
		} else {

			AppController.gotoShowScoreActivity(this);
			finish();
		}

	}

	/**
	 * 結束本轮计时并显示总的计时情况
	 * 
	 * @param v
	 */
	public void finishTiming(View v) {

		String date = (String) app.getMap().get(Constants.TEST_DATE);
		String actv = acTextView.getText().toString().trim();
		int crrentDistance = 0;
		if (!TextUtils.isEmpty(actv)) {
			crrentDistance = Integer.parseInt(actv.replace("米", ""));
		}
		int nowCurrent = (Integer) app.getMap()
				.get(Constants.CURRENT_SWIM_TIME);
		app.getMap().put(Constants.DRAG_NAME_LIST, null);
		int scoresNumber = adapter.getCount();
		int athleteNumber = dragAdapter.getCount();

		if (nowCurrent == 1) {
			if (crrentDistance == 0 && TextUtils.isEmpty(actv)) {
				CommonUtils.showToast(this, mToast, getString(R.string.fill_in_scores_distance));
				return;
			} else if (scoresNumber != athleteNumber) {
				CommonUtils.showToast(this, mToast, getString(R.string.score_num_not_equalwith_athlete_num));
				return;
			} else {
				// 如果这是第一趟并且成绩数目与运动员数目相等，则直接保存到数据库
				matchSuccess(date, nowCurrent, crrentDistance);
			}
		} else {
			/**
			 * 进入统计activity
			 */
//			Intent i = new Intent(this, EachTimeScoreActivity.class);
			// 如果这是第一趟并且成绩数目与运动员数目不相等,则先保存到sp中，统计再做调整
			/**
			 * 通过适配器获取到string
			 */
			String scoresString = JsonTools.creatJsonString(scores);
			dragDatas1 = CommonUtils.getAthleteNamesByAthletes(dragAdapter.getAthletes());
			String athleteJson = JsonTools.creatJsonString(dragDatas1);
			String athleteidJson = JsonTools.creatJsonString(CommonUtils.getAthleteIdsByAthletes(dragAdapter.getAthletes()));
			SpUtil.saveCurrentScoreAndAthlete(this, nowCurrent,
					crrentDistance, scoresString, athleteJson,athleteidJson);
			AppController.gotoEachTimeScoreActivity(MatchScoreActivity.this);
//			startActivity(i);
			finish();
		}

	}

	/**
	 * 退出当前窗体事件
	 * 
	 * @param v
	 */
	public void matchBack(View v) {
		app.getMap().put(Constants.CURRENT_SWIM_TIME, 0);
		finish();
		overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_top_out);
	}

	public void reLoad(View v) {
		scores.clear();
		scores.addAll(originScores);
		adapter.notifyDataSetChanged();
		dragDatas.clear();
		dragDatas1.clear();
		dragDatas.addAll(originNames);
		dragAdapter.notifyDataSetChanged();
	}

	/**
	 * 创建成绩数目与运动员数目不同提示对话框
	 * 
	 * @param context
	 * @param i
	 * @param crrentDistance
	 * @param scoreString
	 * @param athleteString
	 */
	private void createDialog(final Context context, final int i,
			final int crrentDistance, final String scoreString,
			final String athleteString,final String athleteidString) {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setTitle(getString(R.string.system_hint)).setMessage(
				getString(R.string.goto_next_timer_or_adjust_score));
		build.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		build.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				// 暂时保存到SharePreferences
				SpUtil.saveCurrentScoreAndAthlete(context, i,
						crrentDistance, scoreString, athleteString,athleteidString);
//				Intent intent = new Intent(MatchScoreActivity.this,
//						TimerActivity.class);
//				startActivity(intent);
				AppController.gotoTimerActivity(MatchScoreActivity.this);
				finish();
			}
		}).show();

	}

	private class MyScrollListener implements OnScrollListener {

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub
			// 关键代码
			View subView = view.getChildAt(0);
			if (subView != null) {
				final int top = subView.getTop();
				for (ListView item : viewList) {
					item.setSelectionFromTop(firstVisibleItem, top);
				}
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {

			// 关键代码
			if (scrollState == SCROLL_STATE_IDLE
					|| scrollState == SCROLL_STATE_TOUCH_SCROLL) {
				View subView = view.getChildAt(0);
				if (subView != null) {
					final int top = subView.getTop();
					final int position = view.getFirstVisiblePosition();
					for (ListView item : viewList) {
						item.setSelectionFromTop(position, top);
					}
				}
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		// 进入计时界面却不进行成绩匹配而直接返回,要将当前第几次计时置0
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			app.getMap().put(Constants.CURRENT_SWIM_TIME, 0);
			finish();
			overridePendingTransition(R.anim.slide_bottom_in,
					R.anim.slide_top_out);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 发送成绩，找出需要的数据，哪个是空的
	 * @param date
	 */
	private void addScoreRequest(String date) {
		/**
		 * 获取到训练计划
		 */
		SmallPlan sp = new SmallPlan();
		sp.setDistance(plan.getDistance());
		sp.setPool(plan.getPool());
		sp.setExtra(plan.getExtra());
		//新增游泳圈数字段
		sp.setTime(plan.getTime());
		sp.setStrokeNumber(plan.getStrokeNumber());

		List<SmallScore> smallScores = new ArrayList<SmallScore>();
		List<Score> scoresResult = mDbManager.getScoreByDate(date);
		for (Score s : scoresResult) {
			SmallScore smScore = new SmallScore();
			smScore.setScore(s.getScore());
			smScore.setDate(s.getDate());
			smScore.setDistance(s.getDistance());
			smScore.setType(s.getType());
			smScore.setTimes(s.getTimes());
			smallScores.add(smScore);
		}
		/**
		 * 获取运动员id
		 */
		List<Integer> aidList = CommonUtils.getAthleteIdsByAthletes(dragAdapter.getAthletes());
		User user = mDbManager.getUserByUid(userId);
		Map<String, Object> scoreMap = new HashMap<String, Object>();
		scoreMap.put("score", smallScores);
		scoreMap.put("plan", sp);
		scoreMap.put("stroke", plan.getStrokeNumber());
		scoreMap.put("uid", user.getUid());
		scoreMap.put("athlete_id", aidList);
		scoreMap.put("type", 1);
		final String jsonString = JsonTools.creatJsonString(scoreMap);
		Map<String, String> map = new HashMap<String, String>();
		map.put("scoresJson", jsonString);
		VolleyUtil.ResponseListener responseListener = new VolleyUtil.ResponseListener() {
			@Override
			public void onSuccess(String string) {
				loadingDialog.dismiss();
						JSONObject obj;
						try {
							obj = new JSONObject(string);
							int resCode = (Integer) obj.get("resCode");
							int planId = (Integer) obj.get("plan_id");
							if (resCode == 1) {
								CommonUtils.showToast(MatchScoreActivity.this,
										mToast, getString(R.string.synchronized_success));
								ContentValues values = new ContentValues();
								values.put("pid", planId);
								Plan.updateAll(Plan.class, values,
										String.valueOf(plan.getId()));
							} else {
								CommonUtils.showToast(MatchScoreActivity.this,
										mToast, getString(R.string.synchronized_failed));
							}

						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						AppController.gotoShowScoreActivity(MatchScoreActivity.this);
						finish();
			}

			@Override
			public void onError(String string) {

			}
		};
		VolleyUtil.httpJson(CommonUtils.HOSTURL + "addScores", Request.Method.POST,map,responseListener,app);
//		StringRequest stringRequest = new StringRequest(Method.POST,
//				CommonUtils.HOSTURL + "addScores", new Listener<String>() {
//
//					@Override
//					public void onResponse(String response) {
//						// TODO Auto-generated method stub
//						loadingDialog.dismiss();
//						JSONObject obj;
//						try {
//							obj = new JSONObject(response);
//							int resCode = (Integer) obj.get("resCode");
//							int planId = (Integer) obj.get("plan_id");
//							if (resCode == 1) {
//								CommonUtils.showToast(MatchScoreActivity.this,
//										mToast, getString(R.string.synchronized_success));
//								ContentValues values = new ContentValues();
//								values.put("pid", planId);
//								Plan.updateAll(Plan.class, values,
//										String.valueOf(plan.getId()));
//							} else {
//								CommonUtils.showToast(MatchScoreActivity.this,
//										mToast, getString(R.string.synchronized_failed));
//							}
//
//						} catch (JSONException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						AppController.gotoShowScoreActivity(MatchScoreActivity.this);
//						finish();
//					}
//				}, new ErrorListener() {
//
//					@Override
//					public void onErrorResponse(VolleyError error) {
//						// TODO Auto-generated method stub
//					}
//				}) {
//
//			@Override
//			protected Map<String, String> getParams() throws AuthFailureError {
//				// 设置请求参数
//				Map<String, String> map = new HashMap<String, String>();
//				map.put("scoresJson", jsonString);
//				return map;
//			}
//		};
//		stringRequest.setRetryPolicy(new DefaultRetryPolicy(
//				Constants.SOCKET_TIMEOUT,
//				DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//				DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//		mQueue.add(stringRequest);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// TODO Auto-generated method stub
		showPopWindow(position);
		return true;
	}

	private void showPopWindow(final int position) {
		// TODO Auto-generated method stub
		TextView copyView = (TextView) getLayoutInflater().inflate(
				android.R.layout.simple_list_item_1, null);
		copyView.setText(getString(R.string.copy_add));
		copyView.setTextColor(getResources().getColor(R.color.white));
		final PopupWindow pop = new PopupWindow(copyView,
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		pop.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.title_function_bg));
		pop.setOutsideTouchable(true);
		int yoff = mLayout2.getHeight()
				* (position - scoreListView.getFirstVisiblePosition() + 1);
		pop.showAsDropDown(mLayout, scoreListView.getRight() / 2, yoff);
		copyView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				scores.add(position, scores.get(position));
				adapter.notifyDataSetChanged();
				dragDatas.add(position, null);
				dragAdapter.notifyDataSetChanged();
				pop.dismiss();
			}
		});

	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	/**
	 * 进行数据的销毁
	 */
	@Override
	protected void onDestroy() {
//		Log.d("lixinkun","matchscoreactivity Destroy called");
		super.onDestroy();
	}
}
