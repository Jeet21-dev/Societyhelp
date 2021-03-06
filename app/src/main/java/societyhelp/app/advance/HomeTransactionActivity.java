package societyhelp.app.advance;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.List;

import societyhelp.app.R;
import societyhelp.app.util.CustomSerializer;
import societyhelp.dao.SocietyHelpDatabaseFactory;
import societyhelp.dao.mysql.impl.BankStatement;
import societyhelp.dao.mysql.impl.StagingTransaction;

public class HomeTransactionActivity extends DashBoardActivity {

    private ProgressDialog progress;

    public void onButtonClicker(View v) {
        try {
            final BankStatement bankStat = (BankStatement) getIntent().getSerializableExtra("bankStat");

            Intent intent;
            switch (v.getId()) {

                case R.id.transaction_report_activity_btn_view_map_user_alias:
                    List<StagingTransaction> stagingTransactions = SocietyHelpDatabaseFactory.getDBInstance().getAllStaggingTransaction();
                    intent = new Intent(this, ManageExpenditureTypeActivity.UserAliasMappingActivity.class);
                    byte[] data = CustomSerializer.serializeObject(stagingTransactions);
                    intent.putExtra(CONST_PDF_ALL_STAGING_TRANSACTIONS, data);
                    startActivity(intent);
                    break;

                case R.id.transaction_report_activity_btn_view_raw_data:
                    intent = new Intent(this, ManageExpenditureTypeActivity.ViewRawDataActivity.class);
                    intent.putExtra("bankStat", bankStat);
                    startActivity(intent);
                    break;

                case R.id.transaction_report_activity_btn_view_transactions:
                    intent = new Intent(this, ManageFlatWisePayableActivity.TransactionRawDataActivity.class);
                    intent.putExtra("bankStat", bankStat);
                    startActivity(intent);
                    break;

                /*case R.id.transaction_report_activity_btn_upload_raw_data:

                    progress = ProgressDialog.show(this, null, "Uploading Raw data to Database ...", true, false);
                    progress.show();
                    Thread rawDataTaskThread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                SocietyHelpDatabaseFactory.getDBInstance().insertRawData(bankStat.rowdata);
                            }catch (Exception e)
                            {
                                Log.e("Error","Insert Raw data has problem",e);
                            }

                            progress.dismiss();
                            progress.cancel();
                        }
                    });
                    rawDataTaskThread.start();
                    break;
                */
                case R.id.transaction_report_activity_btn_upload_transactions:

                    progress = ProgressDialog.show(this, null, "Uploading transactions to Databases ...", true, false);
                    progress.setCancelable(true);
                    progress.show();
                    Thread transactionsTaskThread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                SocietyHelpDatabaseFactory.getDBInstance().uploadMonthlyTransactions(bankStat);
                                Intent intentAllTrans = new Intent(getApplicationContext(), DetailTransactionViewActivity.class);
                                startActivity(intentAllTrans);
                            } catch (Exception e) {
                                Log.e("Error", "Upload Monthly transaction has problem", e);
                            }
                            progress.dismiss();
                            progress.cancel();
                        }
                    });
                    transactionsTaskThread.start();
                    break;

                case R.id.transaction_report_activity_btn_save_verified_transactions:

                    progress = ProgressDialog.show(this, null, "Uploading verified transactions to Database ...", true, false);
                    progress.setCancelable(true);
                    progress.show();
                    final BankStatement verifiedBankStat = new BankStatement();
                    verifiedBankStat.allTransactions = SocietyHelpDatabaseFactory.getDBInstance().getAllDetailsTransactions();
                    Thread verifiedTransTaskThread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                SocietyHelpDatabaseFactory.getDBInstance().saveVerifiedTransactions(verifiedBankStat);
                            } catch (Exception e) {
                                Log.e("Error", "Insert verified data has problem", e);
                            }

                            progress.dismiss();
                            progress.cancel();
                        }
                    });
                    verifiedTransTaskThread.start();
                    break;

            }
        } catch (Exception e) {
            Log.e("Error", "HomeTransactionActivity has some problem", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_transaction);
        setHeader("", true, false);
    }

}
