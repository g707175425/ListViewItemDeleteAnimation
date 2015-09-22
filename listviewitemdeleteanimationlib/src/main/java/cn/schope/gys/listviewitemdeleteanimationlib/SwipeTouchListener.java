/*
 * Copyright 2014 Niek Haarman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Originally based on Roman Nurik's SwipeDismissListViewTouchListener (https://gist.github.com/romannurik/2980593). */
package cn.schope.gys.listviewitemdeleteanimationlib;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

/**
 */
public abstract class SwipeTouchListener{

    /**
     * TranslationX View property.
     */
    private static final String TRANSLATION_X = "translationX";

    /**
     * Alpha View property.
     */
    private static final String ALPHA = "alpha";

    private static final int MIN_FLING_VELOCITY_FACTOR = 16;

    /**
     * The minimum distance in pixels that should be moved before starting horizontal item movement.
     */
    private final int mSlop;

    /**
     * The minimum velocity to initiate a fling, as measured in pixels per second.
     */
    private final int mMinFlingVelocity;

    /**
     * The maximum velocity to initiate a fling, as measured in pixels per second.
     */
    private final int mMaxFlingVelocity;

    /**
     * The duration of the fling animation.
     */
    private final long mAnimationTime;

    @NonNull
    private final ListViewWrapper mListViewWrapper;

    /**
     * The minimum alpha value of swiped Views.
     */
    private float mMinimumAlpha;

    /**
     * The width of the {@link android.widget.AbsListView} in pixels.
     */
    private int mViewWidth = 1;

    /**
     * The raw X coordinate of the down event.
     */
    private float mDownX;

    /**
     * The raw Y coordinate of the down event.
     */
    private float mDownY;

    /**
     * Indicates whether the user is swiping an item.
     */
    private boolean mSwiping;

    /**
     * Indicates whether the user can dismiss the current item.
     */
    private boolean mCanDismissCurrent;

    /**
     * The {@code VelocityTracker} used in the swipe movement.
     */
    @Nullable
    private VelocityTracker mVelocityTracker;

    /**
     * The parent {@link View} being swiped.
     */
    @Nullable
    private View mCurrentView;

    /**
     * The {@link View} that is actually being swiped.
     */
    @Nullable
    private View mSwipingView;

    /**
     * The current position being swiped.
     */
    private FlingDismissListener.DeleteItemWrapper mCurrentPosition = null;

    /**
     * The number of items in the {@code AbsListView}, minus the pending dismissed items.
     */
    private int mVirtualListCount = -1;

    /**
     * Indicates whether the {@link android.widget.AbsListView} is in a horizontal scroll container.
     * If so, this class will prevent the horizontal scroller from receiving any touch events.
     */
    private boolean mParentIsHorizontalScrollContainer;

    /**
     * The resource id of the {@link View} that may steal touch events from their parents. Useful for example
     * when the {@link android.widget.AbsListView} is in a horizontal scroll container, but not the whole {@code AbsListView} should
     * steal the touch events.
     */
    private int mTouchChildResId;

    /**
     * whether or not a list item can be swiped.
     */
    @Nullable
    private DismissableManager mDismissableManager;

    /**
     * The number of active swipe animations.
     */
    private int mActiveSwipeCount;

    /**
     * Indicates whether swipe is enabled.
     */
    private boolean mSwipeEnabled = true;

    /**
     * Constructs a new {@code SwipeTouchListener} for the given {@link android.widget.AbsListView}.
     */
    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    protected SwipeTouchListener(@NonNull final ListViewWrapper listViewWrapper) {
        ViewConfiguration vc = ViewConfiguration.get(listViewWrapper.getListView().getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * MIN_FLING_VELOCITY_FACTOR;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = listViewWrapper.getListView().getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        mListViewWrapper = listViewWrapper;
    }

    /**
     *
     * @param dismissableManager {@code null} for no restrictions.
     */
    public void setDismissableManager(@Nullable final DismissableManager dismissableManager) {
        mDismissableManager = dismissableManager;
    }

    /**
     * Set the minimum value of the alpha property swiping Views should have.
     *
     * @param minimumAlpha the alpha value between 0.0f and 1.0f.
     */
    public void setMinimumAlpha(final float minimumAlpha) {
        mMinimumAlpha = minimumAlpha;
    }

    /**
     * If the {@link android.widget.AbsListView} is hosted inside a parent(/grand-parent/etc) that can scroll horizontally, horizontal swipes won't
     * work, because the parent will prevent touch-events from reaching the {@code AbsListView}.
     * <p/>
     * Call this method to fix this behavior.
     * Note that this will prevent the parent from scrolling horizontally when the user touches anywhere in a list item.
     */
    public void setParentIsHorizontalScrollContainer() {
        mParentIsHorizontalScrollContainer = true;
        mTouchChildResId = 0;
    }

    /**
     * Sets the resource id of a child view that should be touched to engage swipe.
     * When the user touches a region outside of that view, no swiping will occur.
     *
     * @param childResId The resource id of the list items' child that the user should touch to be able to swipe the list items.
     */
    public void setTouchChild(final int childResId) {
        mTouchChildResId = childResId;
        mParentIsHorizontalScrollContainer = false;
    }

    /**
     * Notifies this {@code SwipeTouchListener} that the adapter contents have changed.
     */
    public void notifyDataSetChanged() {
        if (mListViewWrapper.getAdapter() != null) {
            mVirtualListCount = mListViewWrapper.getCount() - mListViewWrapper.getHeaderViewsCount();
        }
    }

    /**
     * Returns whether the user is currently swiping an item.
     *
     * @return {@code true} if the user is swiping an item.
     */
    public boolean isSwiping() {
        return mSwiping;
    }

    @NonNull
    public ListViewWrapper getListViewWrapper() {
        return mListViewWrapper;
    }

    /**
     * Enables the swipe behavior.
     */
    public void enableSwipe() {
        mSwipeEnabled = true;
    }

    /**
     * Disables the swipe behavior.
     */
    public void disableSwipe() {
        mSwipeEnabled = false;
    }

    /**
     * Flings the {@link View} corresponding to given position out of sight.
     * Calling this method has the same effect as manually swiping an item off the screen.
     *
     * @param position the position of the item in the {@link android.widget.ListAdapter}. Must be visible.
     */
    public boolean fling(FlingDismissListener.DeleteItemWrapper position) {
        int firstVisiblePosition = mListViewWrapper.getFirstVisiblePosition();
        int lastVisiblePosition = mListViewWrapper.getLastVisiblePosition();
        if (position.position < firstVisiblePosition || position.position > lastVisiblePosition) {
            return false;
        }

        View downView = AdapterViewUtil.getViewForPosition(mListViewWrapper, position.position);
        if (downView == null) {
            return false;
        }else{
            flingView(downView, position, true);

            mActiveSwipeCount++;
            mVirtualListCount--;
            return true;
        }
    }

    /**
     * Returns the child {@link View} that was touched, by performing a hit test.
     *
     * @param motionEvent the {@link MotionEvent} to find the {@code View} for.
     *
     * @return the touched {@code View}, or {@code null} if none found.
     */
    @Nullable
    private View findDownView(@NonNull final MotionEvent motionEvent) {
        Rect rect = new Rect();
        int childCount = mListViewWrapper.getChildCount();
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        View downView = null;
        for (int i = 0; i < childCount && downView == null; i++) {
            View child = mListViewWrapper.getChildAt(i);
            if (child != null) {
                child.getHitRect(rect);
                if (rect.contains(x, y)) {
                    downView = child;
                }
            }
        }
        return downView;
    }

    /**
     * Finds out whether the item represented by given position is dismissable.
     *
     * @param position the position of the item.
     *
     * @return {@code true} if the item is dismissable, false otherwise.
     */
    private boolean isDismissable(final int position) {
        if (mListViewWrapper.getAdapter() == null) {
            return false;
        }

        if (mDismissableManager != null) {
            long downId = mListViewWrapper.getAdapter().getItemId(position);
            return mDismissableManager.isDismissable(downId, position);
        }
        return true;
    }

    private void disableHorizontalScrollContainerIfNecessary(@NonNull final MotionEvent motionEvent, @NonNull final View view) {
        if (mParentIsHorizontalScrollContainer) {
            mListViewWrapper.getListView().requestDisallowInterceptTouchEvent(true);
        } else if (mTouchChildResId != 0) {
            mParentIsHorizontalScrollContainer = false;

            final View childView = view.findViewById(mTouchChildResId);
            if (childView != null) {
                final Rect childRect = getChildViewRect(mListViewWrapper.getListView(), childView);
                if (childRect.contains((int) motionEvent.getX(), (int) motionEvent.getY())) {
                    mListViewWrapper.getListView().requestDisallowInterceptTouchEvent(true);
                }
            }
        }
    }

    private boolean handleMoveEvent(@Nullable final View view, @NonNull final MotionEvent motionEvent) {
        if (mVelocityTracker == null || mCurrentView == null) {
            return false;
        }

        mVelocityTracker.addMovement(motionEvent);

        float deltaX = motionEvent.getX() - mDownX;
        float deltaY = motionEvent.getY() - mDownY;

        if (Math.abs(deltaX) > mSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
            if (!mSwiping) {
                mActiveSwipeCount++;
                onStartSwipe(mCurrentView, mCurrentPosition);
            }
            mSwiping = true;
            mListViewWrapper.getListView().requestDisallowInterceptTouchEvent(true);

            /* Cancel ListView's touch (un-highlighting the item) */
            if (view != null) {
                MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                cancelEvent.setAction(MotionEvent.ACTION_CANCEL | motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
                view.onTouchEvent(cancelEvent);
                cancelEvent.recycle();
            }
        }

        if (mSwiping) {
            if (mCanDismissCurrent) {
                ViewHelper.setTranslationX(mSwipingView, deltaX);
                ViewHelper.setAlpha(mSwipingView, Math.max(mMinimumAlpha, Math.min(1, 1 - 2 * Math.abs(deltaX) / mViewWidth)));
            } else {
                ViewHelper.setTranslationX(mSwipingView, deltaX * 0.1f);
            }
            return true;
        }
        return false;
    }

    private boolean handleCancelEvent() {
        if (mVelocityTracker == null || mCurrentView == null) {
            return false;
        }

        if (mCurrentPosition != null && mSwiping) {
            onCancelSwipe(mCurrentView, mCurrentPosition);
            restoreCurrentViewTranslation();
        }

        reset();
        return false;
    }

    private boolean handleUpEvent(@NonNull final MotionEvent motionEvent) {
        if (mVelocityTracker == null || mCurrentView == null) {
            return false;
        }

        if (mSwiping) {
            boolean shouldDismiss = false;
            boolean dismissToRight = false;

            if (mCanDismissCurrent) {
                float deltaX = motionEvent.getX() - mDownX;

                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);

                float velocityX = Math.abs(mVelocityTracker.getXVelocity());
                float velocityY = Math.abs(mVelocityTracker.getYVelocity());

                if (Math.abs(deltaX) > mViewWidth / 2) {
                    shouldDismiss = true;
                    dismissToRight = deltaX > 0;
                } else if (mMinFlingVelocity <= velocityX && velocityX <= mMaxFlingVelocity && velocityY < velocityX) {
                    shouldDismiss = true;
                    dismissToRight = mVelocityTracker.getXVelocity() > 0;
                }
            }


            if (shouldDismiss) {
                beforeViewFling(mCurrentView, mCurrentPosition);
                if (willLeaveDataSetOnFling(mCurrentView, mCurrentPosition)) {
                    mVirtualListCount--;
                }
                flingCurrentView(dismissToRight);
            } else {
                onCancelSwipe(mCurrentView, mCurrentPosition);
                restoreCurrentViewTranslation();
            }
        }

        reset();
        return false;
    }

    /**
     * Flings the pending {@link View} out of sight.
     *
     * @param flingToRight {@code true} if the {@code View} should be flinged to the right, {@code false} if it should be flinged to the left.
     */
    private void flingCurrentView(final boolean flingToRight) {
        if (mCurrentView != null) {
            flingView(mCurrentView, mCurrentPosition, flingToRight);
        }
    }

    /**
     * Flings given {@link View} out of sight.
     *
     * @param view         the parent {@link View}.
     * @param position     the position of the item in the {@link android.widget.ListAdapter} corresponding to the {@code View}.
     * @param flingToRight {@code true} if the {@code View} should be flinged to the right, {@code false} if it should be flinged to the left.
     */
    private void flingView(@NonNull final View view, FlingDismissListener.DeleteItemWrapper position, final boolean flingToRight) {
        if (mViewWidth < 2) {
            mViewWidth = mListViewWrapper.getListView().getWidth();
        }

        View swipeView = getSwipeView(view);
        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(swipeView, TRANSLATION_X, flingToRight ? mViewWidth : -mViewWidth);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(swipeView, ALPHA, 0);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(xAnimator, alphaAnimator);
        animatorSet.setDuration(mAnimationTime);
        animatorSet.addListener(new FlingAnimatorListener(view, position));
        animatorSet.start();
    }

    /**
     * Animates the pending {@link View} back to its original position.
     */
    private void restoreCurrentViewTranslation() {
        if (mCurrentView == null) {
            return;
        }

        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(mSwipingView, TRANSLATION_X, 0);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mSwipingView, ALPHA, 1);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(xAnimator, alphaAnimator);
        animatorSet.setDuration(mAnimationTime);
        animatorSet.addListener(new RestoreAnimatorListener(mCurrentView, mCurrentPosition));
        animatorSet.start();
    }

    /**
     * Resets the fields to the initial values, ready to start over.
     */
    private void reset() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }

        mVelocityTracker = null;
        mDownX = 0;
        mDownY = 0;
        mCurrentView = null;
        mSwipingView = null;
        mCurrentPosition = null;
        mSwiping = false;
        mCanDismissCurrent = false;
    }

    /**
     * Called when the user starts swiping a {@link View}.
     *
     * @param view     the {@code View} that is being swiped.
     * @param position the position of the item in the {@link android.widget.ListAdapter} corresponding to the {@code View}.
     */
    protected void onStartSwipe(@NonNull final View view, FlingDismissListener.DeleteItemWrapper position) {
    }

    /**
     * Called when the swipe movement is canceled. A restore animation starts at this point.
     *
     * @param view     the {@code View} that was swiped.
     * @param position the position of the item in the {@link android.widget.ListAdapter} corresponding to the {@code View}.
     */
    protected void onCancelSwipe(@NonNull final View view, FlingDismissListener.DeleteItemWrapper position) {
    }

    /**
     * Called after the restore animation of a canceled swipe movement ends.
     *
     * @param view     the {@code View} that is being swiped.
     * @param position the position of the item in the {@link android.widget.ListAdapter} corresponding to the {@code View}.
     */
    protected void afterCancelSwipe(@NonNull final View view, FlingDismissListener.DeleteItemWrapper position) {
    }

    /**
     * Called when the user lifted their finger off the screen, and the {@link View} should be swiped away. A fling animation starts at this point.
     *
     * @param view     the {@code View} that is being flinged.
     * @param position the position of the item in the {@link android.widget.ListAdapter} corresponding to the {@code View}.
     */
    protected void beforeViewFling(@NonNull final View view, FlingDismissListener.DeleteItemWrapper position) {
    }

    /**
     * Returns whether flinging the item at given position in the current state
     * would cause it to be removed from the data set.
     *
     * @param view the {@code View} that would be flinged.
     * @param position the position of the item in the {@link android.widget.ListAdapter} corresponding to the {@code View}.
     *
     * @return {@code true} if the item would leave the data set, {@code false} otherwise.
     */
    protected abstract boolean willLeaveDataSetOnFling(@NonNull View view, FlingDismissListener.DeleteItemWrapper position);

    /**
     * Called after the fling animation of a succesful swipe ends.
     * Users of this class should implement any finalizing behavior at this point, such as notifying the adapter.
     *
     * @param view     the {@code View} that is being swiped.
     * @param position the position of the item in the {@link android.widget.ListAdapter} corresponding to the {@code View}.
     */
    protected abstract void afterViewFling(@NonNull View view, FlingDismissListener.DeleteItemWrapper position);

    /**
     * Restores the {@link View}'s {@code alpha} and {@code translationX} values.
     * Users of this class should call this method when recycling {@code View}s.
     *
     * @param view the {@code View} whose presentation should be restored.
     */
    protected void restoreViewPresentation(@NonNull final View view) {
        View swipedView = getSwipeView(view);
        ViewHelper.setAlpha(swipedView, 1);
        ViewHelper.setTranslationX(swipedView, 0);
    }

    /**
     * Returns the number of active swipe animations.
     */
    protected int getActiveSwipeCount() {
        return mActiveSwipeCount;
    }

    /**
     * Returns the {@link View} that should be swiped away. Must be a child of given {@code View}, or the {@code View} itself.
     *
     * @param view the parent {@link View}.
     */
    @NonNull
    protected View getSwipeView(@NonNull final View view) {
        return view;
    }

    private static Rect getChildViewRect(final View parentView, final View childView) {
        Rect childRect = new Rect(childView.getLeft(), childView.getTop(), childView.getRight(), childView.getBottom());
        if (!parentView.equals(childView)) {
            View workingChildView = childView;
            ViewGroup parent;
            while (!(parent = (ViewGroup) workingChildView.getParent()).equals(parentView)) {
                childRect.offset(parent.getLeft(), parent.getTop());
                workingChildView = parent;
            }
        }
        return childRect;
    }

    /**
     * An {@link com.nineoldandroids.animation.Animator.AnimatorListener} that notifies when the fling animation has ended.
     */
    private class FlingAnimatorListener extends AnimatorListenerAdapter {

        @NonNull
        private final View mView;

        private FlingDismissListener.DeleteItemWrapper mPosition;

        private FlingAnimatorListener(@NonNull final View view, FlingDismissListener.DeleteItemWrapper position) {
            mView = view;
            mPosition = position;
        }

        @Override
        public void onAnimationEnd(@NonNull final Animator animation) {
            mActiveSwipeCount--;
            afterViewFling(mView, mPosition);
        }
    }

    /**
     * An {@link com.nineoldandroids.animation.Animator.AnimatorListener} that performs the dismissal animation when the current animation has ended.
     */
    private class RestoreAnimatorListener extends AnimatorListenerAdapter {

        @NonNull
        private View mView;

        private FlingDismissListener.DeleteItemWrapper mPosition;

        private RestoreAnimatorListener(@NonNull View view, FlingDismissListener.DeleteItemWrapper position) {
            mView = view;
            mPosition = position;
        }

        @Override
        public void onAnimationEnd(@NonNull final Animator animation) {
            mActiveSwipeCount--;
            afterCancelSwipe(mView, mPosition);
        }
    }
}
