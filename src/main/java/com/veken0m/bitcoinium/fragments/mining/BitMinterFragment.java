package com.veken0m.bitcoinium.fragments.mining;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veken0m.bitcoinium.R;
import com.veken0m.mining.bitminter.BitMinterData;
import com.veken0m.mining.bitminter.Workers;
import com.veken0m.utils.CurrencyUtils;
import com.veken0m.utils.Utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class BitMinterFragment extends Fragment
{
    private static String pref_bitminterKey = "";
    private static int pref_widgetMiningPayoutUnit = 0;
    private static BitMinterData data = null;
    private final Handler mMinerHandler = new Handler();
    private Boolean connectionFail = false;
    private ProgressDialog minerProgressDialog;
    private final Runnable mGraphView = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                safelyDismiss(minerProgressDialog);
            }
            catch (Exception e)
            {
                // This happens when we try to show a dialog when app is not in the foreground. Suppress it for now
            }
            drawMinerUI();
        }
    };

    public BitMinterFragment()
    {
    }

    private static void readPreferences(Context context)
    {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        // "M3IIJ5OCN2SQKRGRYVIXUFCJGG44DPNJ"
        pref_bitminterKey = prefs.getString("bitminterKey", "");
        pref_widgetMiningPayoutUnit = Integer.parseInt(prefs.getString("widgetMiningPayoutUnitPref", "0"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        readPreferences(getActivity());

        View view = inflater.inflate(R.layout.fragment_table, container, false);
        viewMinerStats(view);
        return view;
    }

    public void onPause()
    {
        super.onPause();
        mMinerHandler.removeCallbacks(mGraphView);
        minerProgressDialog.dismiss();
    }

    void getMinerStats()
    {
        HttpURLConnection urlConnection = null;
        try
        {
            URL url = new URL("https://bitminter.com/api/users?key=" + pref_bitminterKey);
            urlConnection = (HttpURLConnection) url.openConnection();

            ObjectMapper mapper = new ObjectMapper();
            data = mapper.readValue(urlConnection.getInputStream(), BitMinterData.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            connectionFail = true;
        } finally {
            if(urlConnection != null) urlConnection.disconnect();
        }
    }

    private void viewMinerStats(View view)
    {
        if (minerProgressDialog != null && minerProgressDialog.isShowing())
            return;

        Context context = view.getContext();
        if (context != null)
            minerProgressDialog = ProgressDialog.show(context, getString(R.string.working), getString(R.string.retreivingMinerStats), true, false);

        MinerStatsThread gt = new MinerStatsThread();
        gt.start();
    }

    private void safelyDismiss(ProgressDialog dialog)
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
        if (connectionFail)
        {
            final Context context = getActivity();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            Resources res = getResources();
            String text = String.format(res.getString(R.string.error_minerConnection),
                    "BitMinter");
            builder.setMessage(text);
            builder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                        }
                    }
            );

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    void drawMinerUI()
    {
        View view = getView();

        if (view != null)
        {
            try
            {
                TableLayout t1 = (TableLayout) view.findViewById(
                        R.id.minerStatlist);

                Activity activity = getActivity();

                TableRow tr1 = new TableRow(activity);
                TableRow tr2 = new TableRow(activity);
                TableRow tr3 = new TableRow(activity);

                TextView tvBTCRewards = new TextView(activity);
                TextView tvNMCRewards = new TextView(activity);
                TextView tvTotalHashrate = new TextView(activity);

                tr1.setGravity(Gravity.CENTER_HORIZONTAL);
                tr2.setGravity(Gravity.CENTER_HORIZONTAL);
                tr3.setGravity(Gravity.CENTER_HORIZONTAL);

                String RewardsBTC = "BTC Reward: " + CurrencyUtils.formatPayout(data.getBalances().getBTC(), pref_widgetMiningPayoutUnit, "BTC");
                String RewardsNMC = "NMC Reward: " + CurrencyUtils.formatPayout(data.getBalances().getNMC(), pref_widgetMiningPayoutUnit, "NMC");
                String Hashrate = "Total Hashrate: " + data.getHash_rate() + " MH/s\n";

                tvBTCRewards.setText(RewardsBTC);
                tvNMCRewards.setText(RewardsNMC);
                tvTotalHashrate.setText(Hashrate);

                tr1.addView(tvBTCRewards);
                tr2.addView(tvNMCRewards);
                tr3.addView(tvTotalHashrate);

                t1.addView(tr1);
                t1.addView(tr2);
                t1.addView(tr3);

                // End of Non-worker data
                List<Workers> workers = data.getWorkers();
                for (Workers worker : workers)
                {
                    TableRow tr8 = new TableRow(activity);
                    TableRow tr9 = new TableRow(activity);
                    TableRow tr10 = new TableRow(activity);
                    TableRow tr11 = new TableRow(activity);
                    TableRow tr12 = new TableRow(activity);

                    TextView tvMinerName = new TextView(activity);
                    TextView tvHashrate = new TextView(activity);
                    TextView tvAlive = new TextView(activity);
                    TextView tvShares = new TextView(activity);
                    TextView tvStales = new TextView(activity);

                    tr8.setGravity(Gravity.CENTER_HORIZONTAL);
                    tr9.setGravity(Gravity.CENTER_HORIZONTAL);
                    tr10.setGravity(Gravity.CENTER_HORIZONTAL);
                    tr11.setGravity(Gravity.CENTER_HORIZONTAL);
                    tr12.setGravity(Gravity.CENTER_HORIZONTAL);

                    tvMinerName.setText("Miner: " + worker.getName());
                    float hashrate = worker.getHash_rate();
                    tvHashrate.setText("Hashrate: "
                            + Utils.formatDecimal(hashrate, 2, 0, false) + " MH/s");
                    tvAlive.setText("Alive: " + (hashrate > 0.0));
                    tvShares.setText("Shares: "
                            + Utils.formatDecimal(worker.getWork().getBTC()
                            .getTotal_accepted(), 0, 0, true));
                    tvStales.setText("Stales: "
                            + Utils.formatDecimal(worker.getWork().getBTC()
                            .getTotal_rejected(), 0, 0, true));

                    if (hashrate > 0.0)
                    {
                        tvMinerName.setTextColor(Color.GREEN);
                    }
                    else
                    {
                        tvMinerName.setTextColor(Color.RED);
                    }

                    tr8.addView(tvMinerName);
                    tr9.addView(tvHashrate);
                    tr10.addView(tvAlive);
                    tr11.addView(tvShares);
                    tr12.addView(tvStales);

                    t1.addView(tr8);
                    t1.addView(tr9);
                    t1.addView(tr10);
                    t1.addView(tr11);
                    t1.addView(tr12);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private class MinerStatsThread extends Thread
    {
        @Override
        public void run()
        {
            getMinerStats();
            mMinerHandler.post(mGraphView);
        }
    }
}
