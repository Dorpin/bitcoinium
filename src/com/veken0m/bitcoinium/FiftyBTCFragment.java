package com.veken0m.bitcoinium;

import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.veken0m.miningpools.deepbit.DeepBitData;
import com.veken0m.miningpools.deepbit.Worker;

public class FiftyBTCFragment extends SherlockFragment {

	protected static String pref_50BTCKey = "";
	protected static DeepBitData data;
	protected Boolean connectionFail = false;
	private ProgressDialog minerProgressDialog;
	final Handler mMinerHandler = new Handler();

	public FiftyBTCFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readPreferences(getActivity());

		if (pref_50BTCKey.equalsIgnoreCase("")) {

			int duration = Toast.LENGTH_LONG;
			CharSequence text = "Please enter your DeepBit API Token to use MinerStatsActivity with DeepBit";

			Toast toast = Toast.makeText(getActivity(), text, duration);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

			Intent settingsActivity = new Intent(
					getActivity().getBaseContext(), PreferencesActivity.class);
			startActivity(settingsActivity);
		}

		View view = inflater.inflate(R.layout.table_fragment, container, false);
		viewMinerStats(view);
		return view;
	}

	public void getMinerStats(Context context) {

		try {
			//minerdata.setDeepbitData(pref_50BTCKey);
			HttpClient client = new DefaultHttpClient();
			//9a0c3b1d2b9d0040676bb2082628587b

			HttpGet post = new HttpGet("https://50btc.com/en/api/" + pref_50BTCKey + "?text=1");
			HttpResponse response = client.execute(post);
			ObjectMapper mapper = new ObjectMapper();
			data = mapper.readValue(new InputStreamReader(response
					.getEntity().getContent(), "UTF-8"), DeepBitData.class);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			connectionFail = true;
		}
		try {
			//minerdata.setDifficulty();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void viewMinerStats(View view) {
		if (minerProgressDialog != null && minerProgressDialog.isShowing()) {
			return;
		}
		minerProgressDialog = ProgressDialog.show(view.getContext(),
				"Working...", "Retrieving Miner Stats", true, true);

		MinerStatsThread gt = new MinerStatsThread();
		gt.start();
	}

	public class MinerStatsThread extends Thread {

		@Override
		public void run() {
			getMinerStats(getActivity());
			mMinerHandler.post(mGraphView);
		}
	}

	final Runnable mGraphView = new Runnable() {
		@Override
		public void run() {
			safelyDismiss(minerProgressDialog);
			drawMinerUI();
		}
	};

	private void safelyDismiss(ProgressDialog dialog) {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
		if (connectionFail) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Could not retrieve data from "
					+ "DeepBit"
					+ "\n\nPlease make sure that your API Token is entered correctly and that 3G or Wifi is working properly.");
			builder.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alert = builder.create();
			alert.show();
		} else {
		}
	}

	public void drawMinerUI() {

		try {

			TableLayout t1 = (TableLayout) getView().findViewById(
					R.id.minerStatlist);

			TableRow tr1 = new TableRow(getActivity());
			TableRow tr2 = new TableRow(getActivity());
			TableRow tr3 = new TableRow(getActivity());
			TableRow tr4 = new TableRow(getActivity());
			TableRow tr5 = new TableRow(getActivity());
			TableRow tr6 = new TableRow(getActivity());
			TableRow tr7 = new TableRow(getActivity());
			TableRow tr9 = new TableRow(getActivity());

			TextView tvExchangeName = new TextView(getActivity());
			TextView tvBTCRewards = new TextView(getActivity());
			TextView tvBTCPayout = new TextView(getActivity());
			TextView tvHashrate = new TextView(getActivity());

			tr1.setGravity(Gravity.CENTER_HORIZONTAL);
			tr2.setGravity(Gravity.CENTER_HORIZONTAL);
			tr4.setGravity(Gravity.CENTER_HORIZONTAL);
			tr5.setGravity(Gravity.CENTER_HORIZONTAL);
			tr6.setGravity(Gravity.CENTER_HORIZONTAL);
			tr7.setGravity(Gravity.CENTER_HORIZONTAL);
			tr9.setGravity(Gravity.CENTER_HORIZONTAL);
			
			String RewardsBTC = "" + data.getConfirmed_reward().floatValue();
			String Hashrate = "" + data.getHashrate().floatValue();
			String RewardsNMC = "N/A";
			String Payout = "" + data.getPayout_history().floatValue();
			String Alive = "" + data.getWorkers().getWorker(0).getAlive();
			String Shares = "" + data.getWorkers().getWorker(0).getShares();
			String Stales = "" + data.getWorkers().getWorker(0).getStales();
			List<Worker> Workers = data.getWorkers().getWorkers();
			List<String> WorkerNames = data.getWorkers().getNames();
			
			tvBTCRewards.setText("Reward: " + RewardsBTC
					+ " BTC");
			tvBTCPayout.setText("Total Payout: " + Payout
					+ " BTC");
			tvHashrate.setText("Total Hashrate: " + Hashrate
					+ " MH/s");

			tr1.addView(tvExchangeName);
			tr2.addView(tvBTCRewards);
			tr4.addView(tvBTCPayout);
			tr9.addView(tvHashrate);

			t1.addView(tr2);
			t1.addView(tr3);
			t1.addView(tr4);
			t1.addView(tr9);
			t1.addView(tr1);

			// End of Non-worker data

			for (int i = 0; i < Workers.size(); i++) {
				TableRow tr8 = new TableRow(getActivity());
				TableRow tr10 = new TableRow(getActivity());
				TableRow tr11 = new TableRow(getActivity());
				TableRow tr12 = new TableRow(getActivity());

				TextView tvMinerName = new TextView(getActivity());
				TextView tvAlive = new TextView(getActivity());
				TextView tvShares = new TextView(getActivity());
				TextView tvStales = new TextView(getActivity());

				tr8.setGravity(Gravity.CENTER_HORIZONTAL);
				tr10.setGravity(Gravity.CENTER_HORIZONTAL);
				tr11.setGravity(Gravity.CENTER_HORIZONTAL);
				tr12.setGravity(Gravity.CENTER_HORIZONTAL);

				tvMinerName.setText("Miner: "
						+ WorkerNames.get(i));
				tvAlive.setText("Alive: " + Workers.get(i).getAlive());
				tvShares.setText("Shares: " + Workers.get(i).getShares());
				tvStales.setText("Stales: " + Workers.get(i).getStales() + "\n");

				if (Alive.equalsIgnoreCase("true")) {
					tvMinerName.setTextColor(Color.GREEN);
				} else {
					tvMinerName.setTextColor(Color.RED);
				}

				tr8.addView(tvMinerName);
				tr10.addView(tvAlive);
				tr11.addView(tvShares);
				tr12.addView(tvStales);

				t1.addView(tr8);
				t1.addView(tr10);
				t1.addView(tr11);
				t1.addView(tr12);
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	protected static void readPreferences(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		pref_50BTCKey = prefs.getString("50BTCKey", "");
	}

}