package com.scnu.swimmingtrainingsystem.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mobeta.android.dslv.DragSortListView;
import com.scnu.swimmingtrainingsystem.R;
import com.scnu.swimmingtrainingsystem.adapter.ChooseAthleteAdapter;
import com.scnu.swimmingtrainingsystem.adapter.ScoreListAdapter;
import com.scnu.swimmingtrainingsystem.db.DBManager;
import com.scnu.swimmingtrainingsystem.http.JsonTools;
import com.scnu.swimmingtrainingsystem.model.Athlete;
import com.scnu.swimmingtrainingsystem.model.Score;
import com.scnu.swimmingtrainingsystem.model.User;
import com.scnu.swimmingtrainingsystem.util.CommonUtils;
import com.scnu.swimmingtrainingsystem.util.Constants;
import com.scnu.swimmingtrainingsystem.util.SpUtil;
import com.scnu.swimmingtrainingsystem.view.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SimpleDateFormat")
public class MatchSprintScoreActivity extends Activity {

	private MyApplication app;
	private DBManager mDbManager;
	private boolean isConnected;
	private int userId;
	private Toast mToast;
	private View mLayout, mLayout2;
	private RequestQueue mQueue;
	private LoadingDialog loadingDialog;
	private DragSortListView scoreListView;
	private DragSortListView nameListView;
	private ScoreListAdapter adapter;
	private ArrayList<String> scores = new ArrayList<String>();
	private ArrayAdapter<String> dragAdapter;
	private List<ListView> viewList;
	private List<String> dragDatas = new ArrayList<String>();
	private ListView athleteListView;
	private ChooseAthleteAdapter allAthleteAdapter;
	private List<String> athleteNames = new ArrayList<String>();
	private SparseBooleanArray map = new SparseBooleanArray();
	private boolean isSave = false;
	private ImageButton chooseButton;
	private Spinner distanceSpinner;
	private ArrayList<String> originScores = new ArrayList<String>();
	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			String item = dragAdapter.getItem(from);

			dragAdapter.notifyDataSetChanged();
			dragAdapter.remove(item);
			dragAdapter.insert(item, to);
		}
	};

	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
		@Override
		public void remove(int which) {
			if (dragDatas.size() > 1) {
				dragAdapter.remove(dragAdapter.getItem(which));
			} else {
				CommonUtils.showToast(MatchSprintScoreActivity.this, mToast,
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

	private DragSortListView.RemoveListener onRemove2 = new DragSortListView.RemoveListener() {
		@Override
		public void remove(int which) {
			if (scores.size() > 1) {
				scores.remove(which);
			} else {
				CommonUtils.showToast(MatchSprintScoreActivity.this, mToast,
						getString(R.string.leave_at_least_one_score));
			}
			adapter.notifyDataSetChanged();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_match_dash_score);
		try {
			init();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			startActivity(new Intent(this, LoginActivity.class));
		}
	}

	private void init() {
		// TODO Auto-generated method stub
		app = (MyApplication) getApplication();
		app.addActivity(this);
		mDbManager = DBManager.getInstance();
		isConnected = (Boolean) app.getMap().get(Constants.IS_CONNECT_SERVER);
		mQueue = Volley.newRequestQueue(this);
		chooseButton = (ImageButton) findViewById(R.id.add_match_athlete);
		distanceSpinner = (Spinner) findViewById(R.id.spinner_match_dash);
		List<String> dashDistanceList = new ArrayList<String>();
		dashDistanceList.add(getString(R.string.five_meter));
		dashDistanceList.add(getString(R.string.ten_meter));
		dashDistanceList.add(getString(R.string.fif_meter));
		ArrayAdapter<String> spinerAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item, dashDistanceList);
		distanceSpinner.setAdapter(spinerAdapter);
		distanceSpinner.setSelection(2);
//		userId = (Integer) app.getMap().get(Constants.CURRENT_USER_ID);
		scores = getIntent().getStringArrayListExtra("SCORES");
		userId = SpUtil.getUID(MatchSprintScoreActivity.this);
		originScores.addAll(scores);
		List<Athlete> athletes = mDbManager.getAthletes(userId);
		for (int i = 0; i < athletes.size(); i++) {
			map.put(i, false);
		}
		for (Athlete ath : athletes) {
			athleteNames.add(ath.getName());
		}

		mLayout = findViewById(R.id.match_dash_headbar);
		mLayout2 = findViewById(R.id.ll_match_dash2);
		scoreListView = (DragSortListView) findViewById(R.id.matchscore_list);
		nameListView = (DragSortListView) findViewById(R.id.matchName_list);
		nameListView.setDropListener(onDrop);
		nameListView.setRemoveListener(onRemove);
		nameListView.setDragScrollProfile(ssProfile);
		scoreListView.setRemoveListener(onRemove2);
		viewList = new ArrayList<ListView>();
		viewList.add(scoreListView);
		viewList.add(nameListView);
		MyScrollListener mListener = new MyScrollListener();
		scoreListView.setOnScrollListener(mListener);
		nameListView.setOnScrollListener(mListener);
		adapter = new ScoreListAdapter(scoreListView, this, scores);

		dragAdapter = new ArrayAdapter<String>(this, R.layout.drag_list_item,
				R.id.drag_list_item_text, dragDatas);
		scoreListView.setAdapter(adapter);
		nameListView.setAdapter(dragAdapter);
		scoreListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				showPopWindow(position);
				return true;
			}
		});

//		chooseAthlete(chooseButton);

	}

//	public void chooseAthlete(View v) {
//		final NiftyDialogBuilder selectDialog = NiftyDialogBuilder
//				.getInstance(this);
//		Effectstype effect = Effectstype.Fall;
//		selectDialog.setCustomView(R.layout.dialog_choose_athlete, this);
//		Window window = selectDialog.getWindow();
//		athleteListView = (ListView) window.findViewById(R.id.choose_list);
//		allAthleteAdapter = new ChooseAthleteAdapter(this, athleteNames, map);
//		athleteListView.setAdapter(allAthleteAdapter);
//		athleteListView.setOnItemClickListener(new OnItemClickListener() {
//
//			@Override
//			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
//					long arg3) {
//				AdapterHolder holder = (AdapterHolder) arg1.getTag();
//				// 改变CheckBox的状态
//				holder.cb.toggle();
//				if (holder.cb.isChecked()) {
//					if (!dragDatas.contains(allAthleteAdapter
//							.getChooseAthlete().get(arg2)))
//						// 如果checkbox已选并且chosenAthleteList中无该项
//						dragDatas.add(allAthleteAdapter.getChooseAthlete().get(
//								arg2));
//				} else {
//					// 如果checkbox不选择并且chosenAthleteList中有该项
//					if (dragDatas.contains(allAthleteAdapter.getChooseAthlete()
//							.get(arg2)))
//						dragDatas.remove(allAthleteAdapter.getChooseAthlete()
//								.get(arg2));
//				}
//				// 将CheckBox的选中状况记录下来
//				map.put(arg2, holder.cb.isChecked());
//			}
//		});
//		selectDialog.withTitle(getString(R.string.choose_athlete)).withMessage(null)
//				.withIcon(getResources().getDrawable(R.drawable.ic_launcher))
//				.isCancelableOnTouchOutside(false).withDuration(500)
//				.withEffect(effect).withButton1Text(getString(R.string.back))
//				.withButton2Text(Constants.OK_STRING)
//				.setButton1Click(new View.OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						selectDialog.dismiss();
//					}
//				}).setButton2Click(new View.OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						// TODO Auto-generated method stub
//						dragAdapter.notifyDataSetChanged();
//						selectDialog.dismiss();
//					}
//
//				}).show();
//
//		allAthleteAdapter.notifyDataSetChanged();
//	}

	public void saveScores(View v) {
		if (!isSave) {
			isSave = true;
			int scoreNumber = scores.size();
			int athleteNumber = dragDatas.size();
			if (scoreNumber != athleteNumber) {
				CommonUtils.showToast(this, mToast, getString(R.string.score_num_not_equalwith_athlete_num));
			} else {
				String distance = distanceSpinner.getSelectedItem().toString()
						.replace("米", "");
				List<Integer> athIds = new ArrayList<Integer>();
				User user = mDbManager.getUserByUid(userId);
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				String date = sdf.format(new Date());
				for (int i = 0; i < scoreNumber; i++) {
					Athlete athlete = mDbManager.getAthleteByName(userId,
							dragDatas.get(i));
					athIds.add(athlete.getAid());
					Score s = new Score();
					s.setScore(scores.get(i));
					s.setDate(date);
					s.setDistance(Integer.parseInt(distance));
					s.setTimes(1);
					s.setType(3);
					s.setAthlete(athlete);
					s.setUser(user);
					s.save();
				}
				if (isConnected) {
					if (loadingDialog == null) {
						loadingDialog = LoadingDialog.createDialog(this);
						loadingDialog.setMessage(getString(R.string.onSubmitting));
						loadingDialog.setCanceledOnTouchOutside(false);
					}
					loadingDialog.show();
					addScoreRequest(date, athIds);
				} else {
					ShowTipDialog();
				}
			}

		} else {
			CommonUtils.showToast(this, mToast, getString(R.string.dont_need_to_save_again));
			ShowTipDialog();
		}

	}

	private void addScoreRequest(String date, List<Integer> athIds) {
		List<Score> scoresResult = new ArrayList<Score>();
		List<Integer> athList = new ArrayList<Integer>();
		scoresResult.addAll(mDbManager.getScoreByDate(date));
		athList.addAll(athIds);
		User user = mDbManager.getUserByUid(userId);
		Map<String, Object> scoreMap = new HashMap<String, Object>();
		scoreMap.put("score", scoresResult);
		scoreMap.put("plan", null);
		scoreMap.put("uid", user.getUid());
		scoreMap.put("athlete_id", athList);
		scoreMap.put("type", 3);
		final String jsonString = JsonTools.creatJsonString(scoreMap);
		StringRequest stringRequest = new StringRequest(Method.POST,
				CommonUtils.HOSTURL + "addScores", new Listener<String>() {

					@Override
					public void onResponse(String response) {
						// TODO Auto-generated method stub
						Log.i("addScores", response);
						loadingDialog.dismiss();
						JSONObject obj;
						try {
							obj = new JSONObject(response);
							int resCode = (Integer) obj.get("resCode");
							if (resCode == 1) {
								CommonUtils.showToast(
										MatchSprintScoreActivity.this, mToast,
										getString(R.string.submit_succeed));
								ShowTipDialog();
							} else {
								CommonUtils.showToast(
										MatchSprintScoreActivity.this, mToast,
										getString(R.string.submit_failed));
							}

						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}, new ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						// TODO Auto-generated method stub
						loadingDialog.dismiss();
					}
				}) {

			@Override
			protected Map<String, String> getParams() throws AuthFailureError {
				// 设置请求参数
				Map<String, String> map = new HashMap<String, String>();
				map.put("scoresJson", jsonString);
				return map;
			}
		};
		stringRequest.setRetryPolicy(new DefaultRetryPolicy(
				Constants.SOCKET_TIMEOUT,
				DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
				DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		mQueue.add(stringRequest);
	}

	private void createDialog() {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setTitle(getString(R.string.system_hint)).setMessage(getString(R.string.quit_and_dont_save_score));
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
				finish();
				overridePendingTransition(R.anim.slide_bottom_in,
						R.anim.slide_top_out);
			}
		}).show();

	}

	public void matchBack(View v) {
		if (!isSave) {
			createDialog();
		} else {
			finish();
			overridePendingTransition(R.anim.slide_bottom_in,
					R.anim.slide_top_out);
		}

	}

	public void reLoad(View v) {
		scores.clear();
		scores.addAll(originScores);
		adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		// 进入计时界面却不进行成绩匹配而直接返回,要将当前第几次计时置0
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!isSave) {
				createDialog();
			} else {
				finish();
				overridePendingTransition(R.anim.slide_bottom_in,
						R.anim.slide_top_out);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
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

	public void ShowTipDialog() {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setTitle(getString(R.string.system_hint)).setMessage(getString(R.string.score_saved_goto_to_home));
		build.setPositiveButton(Constants.OK_STRING,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						MatchSprintScoreActivity.this.finish();
						overridePendingTransition(R.anim.slide_bottom_in,
								R.anim.slide_top_out);
					}
				});
		build.setNegativeButton(Constants.CANCLE_STRING,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
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
		int yoff = mLayout.getHeight()
				* (position - scoreListView.getFirstVisiblePosition() + 1);
		pop.showAsDropDown(mLayout2, scoreListView.getRight() / 2, yoff);
		copyView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				scores.add(position, scores.get(position));
				adapter.notifyDataSetChanged();
				dragDatas.add(position, "");
				dragAdapter.notifyDataSetChanged();
				pop.dismiss();
			}
		});

	}
}
