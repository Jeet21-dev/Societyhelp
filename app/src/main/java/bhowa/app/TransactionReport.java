package bhowa.app;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import bhowa.dao.BhowaDatabaseFactory;
import bhowa.dao.mysql.impl.BankStatement;
import bhowa.dao.mysql.impl.BhowaTransaction;

public class TransactionReport extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_report);

       final BankStatement bankStat = (BankStatement)getIntent().getSerializableExtra("report");
       final TableLayout tableL = (TableLayout)findViewById(R.id.reportTableLayout);

        for(BhowaTransaction bt : bankStat.allTransactions)
        {
            TableRow row = new TableRow(this);

            TextView srNoCol = new TextView(this); srNoCol.setText(String.valueOf(bt.srNo + ""));row.addView(srNoCol);
            TextView nameCol = new TextView(this); nameCol.setText(bt.name); row.addView(nameCol);
            TextView amountCol = new TextView(this); amountCol.setText(String.valueOf(bt.amount)); row.addView(amountCol);
            TextView dateCol = new TextView(this); dateCol.setText(String.valueOf(bt.transactionDate)); row.addView(dateCol);

            tableL.addView(row);
        }

        final Button uploadTransactionButton = (Button)findViewById(R.id.uploadTransactions);
        uploadTransactionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView uploadText = (TextView) findViewById(R.id.uploadMsg);
                if(BhowaDatabaseFactory.getDBInstance().uploadMonthlyTransactions(bankStat))
                {
                    tableL.removeAllViews();
                    uploadText.setText("Uploaded Successfully :)");
                    BhowaDatabaseFactory.getDBInstance().deleteAllRawData();
                    uploadTransactionButton.setVisibility(View.GONE);
                }
                else
                {
                    uploadText.setText("Uploaded Failed :(");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_transaction_report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
