package cn.schope.gys.listviewitemdeleteanimationlib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public class MyListViewWrapper implements ListViewWrapper {

    @NonNull
    private final ListView mDynamicListView;

    public MyListViewWrapper(@NonNull ListView ListView) {
        mDynamicListView = ListView;
    }

    @NonNull
    @Override
    public ListView getListView() {
        return mDynamicListView;
    }

    @Nullable
    @Override
    public View getChildAt(final int index) {
        return mDynamicListView.getChildAt(index);
    }

    @Override
    public int getFirstVisiblePosition() {
        return mDynamicListView.getFirstVisiblePosition();
    }

    @Override
    public int getLastVisiblePosition() {
        return mDynamicListView.getLastVisiblePosition();
    }

    @Override
    public int getCount() {
        return mDynamicListView.getCount();
    }

    @Override
    public int getChildCount() {
        return mDynamicListView.getChildCount();
    }

    @Override
    public int getHeaderViewsCount() {
        return mDynamicListView.getHeaderViewsCount();
    }

    @Override
    public int getPositionForView(@NonNull final View view) {
        return mDynamicListView.getPositionForView(view);
    }

    @Nullable
    @Override
    public ListAdapter getAdapter() {
        return mDynamicListView.getAdapter();
    }

    @Override
    public void smoothScrollBy(final int distance, final int duration) {
        mDynamicListView.smoothScrollBy(distance, duration);
    }

}
