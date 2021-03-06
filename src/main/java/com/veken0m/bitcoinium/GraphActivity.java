package com.veken0m.bitcoinium;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.veken0m.bitcoinium.exchanges.ExchangeProperties;
import com.veken0m.bitcoinium.preferences.GraphPreferenceActivity;
import com.veken0m.utils.Constants;
import com.veken0m.utils.CurrencyUtils;
import com.veken0m.utils.ExchangeUtils;
import com.veken0m.utils.Utils;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.marketdata.Trades;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.veken0m.utils.ExchangeUtils.getDropdownItems;

public class GraphActivity extends BaseActivity implements OnItemSelectedListener, SwipeRefreshLayout.OnRefreshListener
{
    private static final Handler mOrderHandler = new Handler(Looper.getMainLooper());
    private static Boolean connectionFail = true;
    private static Boolean noTradesFound = false;

    private static SharedPreferences prefs = null;

    private static CurrencyPair currencyPair = null;
    private static String exchangeName = "";
    private static ExchangeProperties exchange = null;
    private static Boolean exchangeChanged = false;
    private static Boolean pref_scaleMode = null;
    /**
     * Variables required for LineGraphView
     */
    private GraphView graphView = null;

    private final Runnable mGetTrades = new Runnable()
    {
        @Override
        public void run()
        {
            generatePriceGraph();
        }
    };


    /**
     * mGraphView run() is called when our GraphThread is finished
     */
    private final Runnable mGraphView = new Runnable()
    {
        @Override
        public void run()
        {
            if (graphView != null && !connectionFail && !noTradesFound)
            {
                LinearLayout graphLinearLayout = (LinearLayout) findViewById(R.id.graphView);
                graphLinearLayout.removeAllViews(); // make sure layout has no child
                graphLinearLayout.addView(graphView);
            }
            else if (noTradesFound)
            {
                createPopup(getString(R.string.msg_noTradesFound));
            }
            else
            {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.error_exchangeConnection), res.getString(R.string.trades), exchangeName);
                createPopup(text);
            }
            swipeLayout.setRefreshing(false);
        }
    };

    private static void readPreferences()
    {
        pref_scaleMode = prefs.getBoolean("graphscalePref", false);
        currencyPair = CurrencyUtils.stringToCurrencyPair(prefs.getString(exchange.getIdentifier() + "CurrencyPref", exchange.getDefaultCurrency()));
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.show();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            exchange = new ExchangeProperties(this, extras.getString("exchange"));
        else
            exchange = new ExchangeProperties(this, prefs.getString("defaultExchangePref", Constants.DEFAULT_EXCHANGE));

        if (!exchange.supportsTrades())
            exchange = new ExchangeProperties(this, Constants.DEFAULT_EXCHANGE);

        exchangeName = exchange.getExchangeName();

        readPreferences();
        setContentView(R.layout.activity_graph);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.graph_swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.holo_blue_light);

        createExchangeDropdown();
        createCurrencyDropdown();
        onRefresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_preferences:
                startActivity(new Intent(this, GraphPreferenceActivity.class));
                return true;
            case R.id.action_refresh:
                onRefresh();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * generatePriceGraph prepares price graph of all the values available from
     * the API It connects to exchange, reads the JSON, and plots a GraphView of
     * it
     */
    private void generatePriceGraph()
    {
        Trades trades = null;

        try
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            trades = ExchangeUtils.getMarketData(exchange).getTrades(currencyPair);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            List<Trade> tradesList = trades.getTrades();

            float[] values = new float[tradesList.size()];
            LineGraphSeries<DataPoint> data = new LineGraphSeries<>();

            Date date = tradesList.get(0).getTimestamp();
            final int tradesListSize = tradesList.size();
            for (int i = 0; i < tradesListSize; i++)
            {
                Trade trade = tradesList.get(i);
                // if datapoints are less than 1 min apart, skip
                if((trade.getTimestamp().getTime() - date.getTime() < 60000L) && i!=0)
                    continue;

                date = trade.getTimestamp();
                values[i] = trade.getPrice().floatValue();

                data.appendData(new DataPoint(trade.getTimestamp(), values[i]),true,1000000);
            }

            data.setThickness(10);
            //data.setDrawDataPoints(true);

            graphView = new GraphView(this);
            graphView.setTitle(exchangeName + ": " + currencyPair.toString());
            graphView.setTitleTextSize(25);
            graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    return isValueX ? Utils.dateFormat(getBaseContext(), (long) value) : super.formatLabel(value, false);
                }
            });

            graphView.addSeries(data);

            //graphView.getGridLabelRenderer().setNumHorizontalLabels(4);
            //graphView.getGridLabelRenderer().setNumVerticalLabels(10);
            graphView.getGridLabelRenderer().setTextSize(18);

            double largest = data.getHighestValueX();
            double smallest = data.getLowestValueX();
            double windowSize = (largest-smallest)/4;

            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.getViewport().setMinX(smallest+windowSize);
            graphView.getViewport().setMaxX(largest);

            // enable scaling and scrolling
            graphView.getViewport().setScalable(true);
            graphView.getViewport().setScrollable(true);

            if (!pref_scaleMode)
            {
                graphView.getViewport().setYAxisBoundsManual(true);
                graphView.getViewport().setMinY(data.getLowestValueY()*0.9999);
                graphView.getViewport().setMaxY(data.getHighestValueY()*1.0001);
            }
            connectionFail = false;
            noTradesFound = false;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            noTradesFound = true;
        }
        catch (Exception e)
        {
            connectionFail = true;
            e.printStackTrace();
        }
    }

    private void createPopup(String pMessage)
    {
        try
        {
            if (dialog == null || !dialog.isShowing())
            {
                // Display error Dialog
                dialog = Utils.errorDialog(this, pMessage);
            }
        }
        catch (WindowManager.BadTokenException e)
        {
            // This happens when we try to show a dialog when app is not in the foreground. Suppress it for now
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (graphView != null)
        {
            LinearLayout graphLinearLayout = (LinearLayout) findViewById(R.id.graphView);
            graphLinearLayout.removeAllViews();
            graphLinearLayout.addView(graphView);
        }
        else
        {
            viewGraph();
        }
    }

    private void viewGraph()
    {
        swipeLayout.setRefreshing(true);
        mOrderHandler.post(mGetTrades);
        mOrderHandler.post(mGraphView);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        CurrencyPair prevCurrencyPair = currencyPair;
        String prevExchangeName = exchangeName;

        switch (parent.getId())
        {
            case R.id.graph_exchange_spinner:
                exchangeName = (String) parent.getItemAtPosition(pos);
                exchangeChanged = prevExchangeName != null && exchangeName != null && !exchangeName.equals(prevExchangeName);
                if (exchangeChanged)
                {
                    exchange = new ExchangeProperties(this, exchangeName);
                    currencyPair = CurrencyUtils.stringToCurrencyPair(prefs.getString(exchange.getIdentifier() + "CurrencyPref", exchange.getDefaultCurrency()));
                    createCurrencyDropdown();
                }
                break;
            case R.id.graph_currency_spinner:
                currencyPair = CurrencyUtils.stringToCurrencyPair((String) parent.getItemAtPosition(pos));
                break;
        }

        if (prevCurrencyPair != null && currencyPair != null && !currencyPair.equals(prevCurrencyPair) || exchangeChanged)
            viewGraph();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0)
    {
        // Do nothing
    }

    void createExchangeDropdown()
    {
        // Re-populate the dropdown menu
        List<String> exchanges = getDropdownItems(this, ExchangeProperties.ItemType.TRADES_ENABLED).first;
        Spinner spinner = (Spinner) findViewById(R.id.graph_exchange_spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exchanges);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(this);

        int index = exchanges.indexOf(exchange.getExchangeName());
        spinner.setSelection(index);
    }

    void createCurrencyDropdown()
    {
        // Re-populate the dropdown menu
        String[] currencies = exchange.getCurrencies();
        Spinner spinner = (Spinner) findViewById(R.id.graph_currency_spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(this);

        if (exchangeChanged)
        {
            int index = Arrays.asList(currencies).indexOf(currencyPair.toString());
            spinner.setSelection(index);
        }
    }

    @Override
    public void onRefresh()
    {
        LinearLayout graphLinearLayout = (LinearLayout) findViewById(R.id.graphView);
        graphLinearLayout.removeAllViews();
        viewGraph();
        //swipeLayout.setRefreshing(false);
    }
}
