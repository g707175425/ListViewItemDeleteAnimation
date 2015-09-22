package cn.schope.gys.listviewitemdeleteanimation;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import cn.schope.gys.listviewitemdeleteanimationlib.MyListViewWrapper;
import cn.schope.gys.listviewitemdeleteanimationlib.OnDismissCallback;
import cn.schope.gys.listviewitemdeleteanimationlib.SwipeDismissTouchListener;

public class MainActivity extends Activity implements OnDismissCallback {

    private SwipeDismissTouchListener swipeDismissTouchListener;
    private ArrayList<String> strs = new ArrayList<String>();
    private MyAdapter adapter;

    {
        for (int i = 0;i<20;i++){
            strs.add("我是第:"+i+"个Item");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView lv = (ListView) findViewById(R.id.lv);
        View but = findViewById(R.id.but);
        adapter = new MyAdapter();
        lv.setAdapter(adapter);
        lv.setDivider(null);
        swipeDismissTouchListener = new SwipeDismissTouchListener(new MyListViewWrapper(lv),this);

        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<SwipeDismissTouchListener.DeleteItemWrapper> deleteItems = new ArrayList<SwipeDismissTouchListener.DeleteItemWrapper>();
                for(int i = 0;i<strs.size();i++){
                    deleteItems.add(new SwipeDismissTouchListener.DeleteItemWrapper(i,strs.get(i)));
                }
                swipeDismissTouchListener.dismiss(deleteItems);
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                swipeDismissTouchListener.dismissOne(position,strs.get(position));
            }
        });
    }

    @Override
    public void onDismiss(@NonNull ViewGroup listView, @NonNull SwipeDismissTouchListener.DeleteItemWrapper[] reverseSortedPositions) {
        //当item删除动画结束时执行这里

        for (SwipeDismissTouchListener.DeleteItemWrapper deleteItem : reverseSortedPositions){
            //由于每次删除一些item所在的position都会改变,所以必须使用对象来删除.
            strs.remove(deleteItem.item);
        }
        adapter.notifyDataSetChanged();

    }

    private class MyAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return strs.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout frameLayout = new FrameLayout(MainActivity.this);
            TextView textView = new TextView(MainActivity.this);
            textView.setText(strs.get(position));
            frameLayout.addView(textView);
            textView.getLayoutParams().height = (int) dp2px(30);
            return frameLayout;
        }
    }

    public float dp2px(float dp){
        return dp * getResources().getDisplayMetrics().density;
    }

}
