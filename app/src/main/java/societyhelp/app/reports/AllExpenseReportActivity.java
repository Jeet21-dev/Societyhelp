package societyhelp.app.reports;

import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import societyhelp.app.DashBoardActivity;
import societyhelp.app.R;
import societyhelp.app.util.CustomSerializer;
import societyhelp.app.util.SocietyHelpConstant;
import societyhelp.dao.mysql.impl.ExpenseType;
import societyhelp.dao.mysql.impl.TransactionOnBalanceSheet;

public class AllExpenseReportActivity extends DashBoardActivity  implements SocietyHelpConstant {

    protected HorizontalBarChart mChart;
    private float[] yData = null;
    private String[] xData = null;
    private HashMap<String, float[]> labelValueData= new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_expense_report);
        setHeader(getString(R.string.title_activity_all_expense_report), true, false);
        TreeMap<ExpenseType.ExpenseTypeConst, List<TransactionOnBalanceSheet>> apartmentExpense = null;
        try {
            byte[] sObjet = (byte[]) getIntent().getSerializableExtra(CONST_APARTMENT_EXPENSE_DATA);
            apartmentExpense = (TreeMap<ExpenseType.ExpenseTypeConst, List<TransactionOnBalanceSheet>>) CustomSerializer.deserializeObject(sObjet);
            yData = new float[apartmentExpense.keySet().size()];
            xData = new String[apartmentExpense.keySet().size()];
            int i=0;
            for(ExpenseType.ExpenseTypeConst eType : apartmentExpense.keySet()){
                xData[i] = eType.toString();
                float[] values = new float[apartmentExpense.get(eType).size()];
                int j=0;
                for(TransactionOnBalanceSheet tran : apartmentExpense.get(eType)) {
                    yData[i] += tran.amount;
                    values[j++] = tran.amount;
                }
                labelValueData.put(eType.toString(), values);
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mChart = (HorizontalBarChart) findViewById(R.id.barChatExpense);
        setData();

    }

    private void setData() {

        ArrayList<BarEntry> yVals1 = new ArrayList<BarEntry>();
        float spaceForBar = 5f;
        float barWidth = 4f;
        float xAxisMax = 0;
        for (int i = 0; i < yData.length; i++) {
            BarEntry be = new BarEntry(yData[i],i);
            yVals1.add(be);
            if(yData[i] > xAxisMax) xAxisMax = yData[i];
        }
        BarDataSet set = new BarDataSet(yVals1, "Apartment Expense (2011-2017)");

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = 0; i < xData.length; i++)
            xVals.add(xData[i].replaceAll("_"," "));

        BarData barData = new BarData(xVals, set);
        mChart.setData(barData);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(false);
        mChart.getAxisLeft().setDrawLabels(false);
        mChart.getAxisRight().setDrawLabels(false);

        XAxis xl = mChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setDrawAxisLine(true);
        xl.setDrawGridLines(true);
        xl.setGridLineWidth(0.3f);

        YAxis yl = mChart.getAxisLeft();
        yl.setDrawAxisLine(true);
        yl.setDrawGridLines(true);
        yl.setGridLineWidth(0.3f);
        yl.setAxisMinValue(0f); // this replaces setStartAtZero(true)

        YAxis yr = mChart.getAxisRight();
        yr.setDrawAxisLine(true);
        yr.setDrawGridLines(false);
        yr.setAxisMinValue(0f); // this replaces setStartAtZero(true)

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        l.setFormSize(8f);
        l.setXEntrySpace(4f);

    }
}