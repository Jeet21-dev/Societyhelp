package societyhelp.app.advance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;

import societyhelp.app.R;
import societyhelp.app.util.FileChooser;
import societyhelp.dao.mysql.impl.BankStatement;
import societyhelp.parser.SocietyHelpParserFactory;

public class FileBrowserActivity extends Activity implements View.OnClickListener {

    private Button Browse;
    private TextView filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        Browse = (Button) findViewById(R.id.browse);
        filePath = (TextView) findViewById(R.id.file_path);
        Browse = (Button) findViewById(R.id.browse);
        Browse.setOnClickListener(this);


    }

    @Override
    public void onClick(final View v) {

        new FileChooser(this).setFileListener(new FileChooser.FileSelectedListener() {
                @Override public void fileSelected(final File file) {
                    String path = file.getPath();
                    BankStatement bankStat = (BankStatement) SocietyHelpParserFactory.getInstance().getAllTransaction(path);
                    Intent transactionReportIntent = new Intent(v.getContext(), HomeTransactionActivity.class);
                    transactionReportIntent.putExtra("bankStat",bankStat);
                    startActivityForResult(transactionReportIntent, 0);
                }}).showDialog();
    }
}
