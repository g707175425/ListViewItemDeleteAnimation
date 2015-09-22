#ListViewItemDeleteAnimation
###ListView的item删除动画,支持多条,单条删除
###预览效果:

![预览图](https://github.com/g707175425/ListViewItemDeleteAnimation/blob/master/GIF.gif)

###使用方法:
使用FlingDismissListener和MyListViewWrapper将ListView包装起来,然后使用FlingDismissListener操作item的消失
如:        
	FlingDismissListener flingDismissListener = new FlingDismissListener(MyListViewWrapper wrapper,OnDismissCallback callBack);//OnDismissCallback是用来在动画结束时,删除真正数据的回调
	flingDismissListener.dismissOne(int position,Object item);//删除一条,这里的第二个参数是要删除的对象(因为删除多条数据时position会变化,所以使用对象来从list中删除)
	
	ArrayList<FlingDismissListener.DeleteItemWrapper> deleteItems = new ArrayList<FlingDismissListener.DeleteItemWrapper>();
	                for(int i = 0;i<strs.size();i++){
	                    deleteItems.add(new FlingDismissListener.DeleteItemWrapper(i,strs.get(i)));
	                }
	                flingDismissListener.dismiss(deleteItems);//删除多条数据,每个item都要用DeleteItemWrapper包装一下.
	@Override
    public void onDismiss(@NonNull ViewGroup listView, @NonNull FlingDismissListener.DeleteItemWrapper[] reverseSortedPositions) {//在动画消失时,要被删掉的数据将会以DeleteItemWrapper的数组形式传进来.
        //当item删除动画结束时执行这里

        for (FlingDismissListener.DeleteItemWrapper deleteItem : reverseSortedPositions){
            //由于每次删除一些item所在的position都会改变,所以必须使用对象来删除.
            strs.remove(deleteItem.item);
        }
        adapter.notifyDataSetChanged();

    }