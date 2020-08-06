package me.donlis.mcalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MCalendarView calendar = findViewById(R.id.calendar);
        calendar.setDateSelectListener(new MCalendarView.DateSelectListener() {
            @Override
            public void onSelect(int year, int month, int day) {
                Toast.makeText(MainActivity.this,year+"-"+month+"-"+day,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
