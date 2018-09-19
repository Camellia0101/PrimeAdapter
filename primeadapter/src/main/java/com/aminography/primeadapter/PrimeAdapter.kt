package com.aminography.primeadapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.support.annotation.ColorInt
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SnapHelper
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.ViewGroup
import com.aminography.primeadapter.callback.OnRecyclerViewItemClickListener
import com.aminography.primeadapter.callback.PrimeDelegate
import com.aminography.primeadapter.divider.SkipDividerItemDecorator
import com.aminography.primeadapter.draghelper.DragHelper
import com.aminography.primeadapter.draghelper.IDragHelperCallback
import com.aminography.primeadapter.draghelper.OnRecyclerViewItemDragListener
import com.aminography.primeadapter.exception.ViewHolderNotFoundException
import com.aminography.primeadapter.tools.PrimeAdapterUtils
import java.util.*


/**
 * Created by aminography on 6/6/2018.
 */
abstract class PrimeAdapter : RecyclerView.Adapter<PrimeViewHolder<PrimeDataHolder>>(), PrimeDelegate {

    protected lateinit var context: Context
    protected lateinit var layoutInflater: LayoutInflater
    private var dataList: MutableList<PrimeDataHolder> = ArrayList()
    private var itemClickListener: OnRecyclerViewItemClickListener? = null
    private var itemDragListener: OnRecyclerViewItemDragListener? = null
    private var itemTouchHelper: ItemTouchHelper? = null
    private var dragHelper: DragHelper? = null
    private var recyclerView: RecyclerView? = null
    protected var recycledViewPool: RecyclerView.RecycledViewPool? = null
    private var isDraggable: Boolean = false
    private var isExpandable: Boolean = false
    private var isSwipeToDismissEnabled: Boolean = false

    override fun onBindViewHolder(viewHolder: PrimeViewHolder<PrimeDataHolder>, position: Int) {
        val dataHolder = dataList[position]
        dataHolder.listPosition = position
        dataHolder.expanded = dataHolder.expanded && isExpandable
        viewHolder.dataHolder = dataHolder
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrimeViewHolder<PrimeDataHolder> {
        val dataHolderClass = PrimeAdapterUtils.reverseViewTypeMap[viewType]
        val viewHolder = makeViewHolder(dataHolderClass)
        if (viewHolder == null) {
            throw ViewHolderNotFoundException(dataHolderClass, this::class.java)
        } else {
            @Suppress("UNCHECKED_CAST")
            return viewHolder as PrimeViewHolder<PrimeDataHolder>
        }
    }

    abstract fun makeViewHolder(dataHolderClass: Class<*>?): PrimeViewHolder<*>?

    override fun getInflater(): LayoutInflater = layoutInflater

    override fun getParentView(): ViewGroup? = recyclerView

    override fun getOnItemClickListener(): OnRecyclerViewItemClickListener? = itemClickListener

    override fun getViewPool(): RecyclerView.RecycledViewPool? = recycledViewPool

    override fun isDraggable(): Boolean {
        return isDraggable
    }

    fun setDraggable(isDraggable: Boolean) {
        this.isDraggable = isDraggable
        if (isDraggable) initItemTouchHelper()
    }

    fun setIsSwipeToDismissEnabled(isSwipeToDismissEnabled: Boolean) {
        this.isSwipeToDismissEnabled = isSwipeToDismissEnabled
        if (isSwipeToDismissEnabled) initItemTouchHelper()
    }

    private fun initItemTouchHelper() {
        if (itemTouchHelper == null) {
            val itemTouchHelperCallback = object : IDragHelperCallback {

                override fun isOnlySameViewTypeCanReplaceable(): Boolean = true

                override fun isLongPressDragEnabled(): Boolean = false

                override fun isItemViewSwipeEnabled(): Boolean = isSwipeToDismissEnabled

                override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
                    dataList.add(toPosition, dataList.removeAt(fromPosition))

                    if (fromPosition < toPosition) for (i in fromPosition..toPosition) dataList[i].listPosition = i
                    else for (i in toPosition..fromPosition) dataList[i].listPosition = i

                    notifyItemMoved(fromPosition, toPosition)
                    itemDragListener?.onItemMoved(fromPosition, toPosition)
                    return true
                }

                override fun onItemSwiped(position: Int, direction: Int) {
                    removeItem(position, true)
                }
            }

            dragHelper = DragHelper(itemTouchHelperCallback)
            itemTouchHelper = ItemTouchHelper(dragHelper!!)
            itemTouchHelper?.attachToRecyclerView(recyclerView)
        }
    }

    override fun toggleExpansion(dataHolder: PrimeDataHolder): Boolean {
        if (isExpandable) {
            dataHolder.expanded = !dataHolder.expanded
            notifyItemChanged(dataHolder.listPosition)
        }
        return isExpandable
    }

    override fun isExpandable(): Boolean {
        return isExpandable
    }

    fun setExpandable(isExpandable: Boolean) {
        this.isExpandable = isExpandable
        (recyclerView?.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = true
        notifyDataSetChanged()
    }

    fun setDivider(dividerDrawable: Drawable?, insetLeft: Int = 0, insetTop: Int = 0, insetRight: Int = 0, insetBottom: Int = 0) {
        val itemDecorationCount = recyclerView?.itemDecorationCount ?: 0
        if (itemDecorationCount > 0) {
            recyclerView?.removeItemDecorationAt(0)
//            for (i in 0..(itemDecorationCount - 1)) recyclerView?.removeItemDecorationAt(i)
        }
        if (recyclerView?.layoutManager is LinearLayoutManager) {
            dividerDrawable?.let {
                val insetDrawable = InsetDrawable(it, insetLeft, insetTop, insetRight, insetBottom)
                val dividerItemDecoration = SkipDividerItemDecorator(insetDrawable)
                recyclerView?.addItemDecoration(dividerItemDecoration)
            }
        }
    }

    fun setDivider(@ColorInt color: Int = Color.parseColor("#BDBDBD"), thickness: Int = 1, insetLeft: Int = 0, insetTop: Int = 0, insetRight: Int = 0, insetBottom: Int = 0) {
        val dividerDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setSize(thickness, thickness)
            setColor(color)
        }
        setDivider(dividerDrawable, insetLeft, insetTop, insetRight, insetBottom)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
    }

    override fun ontDragReleased(viewHolder: RecyclerView.ViewHolder) {
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = dataList.size

    override fun getItemViewType(position: Int): Int = PrimeAdapterUtils.viewTypeMap[dataList[position]::class.java]!!

    fun getItem(position: Int): PrimeDataHolder = dataList[position]

    fun addItem(dataHolder: PrimeDataHolder, position: Int = 0, animate: Boolean = true) {
        dataList.add(position, dataHolder)
        for (i in position..(dataList.size - 1)) dataList[i].listPosition = i

        if (animate) {
            notifyItemInserted(position)
        } else {
            notifyDataSetChanged()
        }
    }

    fun addItemToLast(dataHolder: PrimeDataHolder, animate: Boolean = true) = addItem(dataHolder, itemCount, animate)

    fun removeItem(position: Int, animate: Boolean) {
        dataList.removeAt(position)
        for (i in position..(dataList.size - 1)) dataList[i].listPosition = i

        if (animate) {
            notifyItemRemoved(position)
        } else {
            notifyDataSetChanged()
        }
    }

    fun removeItem(dataHolder: PrimeDataHolder, animate: Boolean) {
        val position = dataHolder.listPosition
        if (position != RecyclerView.NO_POSITION) {
            removeItem(position, animate)
        }
    }

    fun replaceDataList(modelList: List<PrimeDataHolder>) {
        dataList.clear()
        dataList.addAll(modelList)
        notifyDataSetChanged()
    }

    class AdapterBuilder(private val recyclerView: RecyclerView) {

        private var layoutManager: RecyclerView.LayoutManager? = null
        private var snapHelper: SnapHelper? = null
        private var recycledViewPool: RecyclerView.RecycledViewPool? = null
        private var itemClickListener: OnRecyclerViewItemClickListener? = null
        private var itemDragListener: OnRecyclerViewItemDragListener? = null
        private var dividerDrawable: Drawable? = null
        private var dividerDrawableInsetLeft: Int = 0
        private var dividerDrawableInsetTop: Int = 0
        private var dividerDrawableInsetRight: Int = 0
        private var dividerDrawableInsetBottom: Int = 0
        private var hasFixedSize: Boolean? = null
        private var isNestedScrollingEnabled: Boolean? = null
        private var isDraggable: Boolean? = null
        private var isExpandable: Boolean? = null
        private var isSwipeToDismissEnabled: Boolean? = null
        private var set: Boolean = false

        fun set(): AdapterBuilder {
            set = true
            return this
        }

        fun setHasFixedSize(hasFixedSize: Boolean): AdapterBuilder {
            this.hasFixedSize = hasFixedSize
            return this
        }

        fun setDraggable(isDraggable: Boolean): AdapterBuilder {
            this.isDraggable = isDraggable
            return this
        }

        fun setExpandable(isExpandable: Boolean): AdapterBuilder {
            this.isExpandable = isExpandable
            return this
        }

        fun setIsSwipeToDismissEnabled(isSwipeToDismissEnabled: Boolean): AdapterBuilder {
            this.isSwipeToDismissEnabled = isSwipeToDismissEnabled
            return this
        }

        fun setIsNestedScrollingEnabled(isNestedScrollingEnabled: Boolean): AdapterBuilder {
            this.isNestedScrollingEnabled = isNestedScrollingEnabled
            return this
        }

        fun setViewPool(recycledViewPool: RecyclerView.RecycledViewPool?): AdapterBuilder {
            this.recycledViewPool = recycledViewPool
            return this
        }

        fun setLayoutManager(layoutManager: RecyclerView.LayoutManager): AdapterBuilder {
            this.layoutManager = layoutManager
            return this
        }

        fun setSnapHelper(snapHelper: SnapHelper): AdapterBuilder {
            this.snapHelper = snapHelper
            return this
        }

        fun setItemClickListener(itemClickListener: OnRecyclerViewItemClickListener): AdapterBuilder {
            this.itemClickListener = itemClickListener
            return this
        }

        fun setItemDragListener(itemDragListener: OnRecyclerViewItemDragListener): AdapterBuilder {
            this.itemDragListener = itemDragListener
            return this
        }

        fun setDivider(dividerDrawable: Drawable?, insetLeft: Int = 0, insetTop: Int = 0, insetRight: Int = 0, insetBottom: Int = 0): AdapterBuilder {
            this.dividerDrawable = dividerDrawable
            dividerDrawableInsetLeft = insetLeft
            dividerDrawableInsetTop = insetTop
            dividerDrawableInsetRight = insetRight
            dividerDrawableInsetBottom = insetBottom
            return this
        }

        fun setDivider(@ColorInt color: Int = Color.parseColor("#BDBDBD"), thickness: Int = 1, insetLeft: Int = 0, insetTop: Int = 0, insetRight: Int = 0, insetBottom: Int = 0): AdapterBuilder {
            this.dividerDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setSize(thickness, thickness)
                setColor(color)
            }
            dividerDrawableInsetLeft = insetLeft
            dividerDrawableInsetTop = insetTop
            dividerDrawableInsetRight = insetRight
            dividerDrawableInsetBottom = insetBottom
            return this
        }

        fun <T : PrimeAdapter> build(adapterClass: Class<T>): T {
            val t = adapterClass.newInstance()
            t.recyclerView = recyclerView
            t.context = recyclerView.context
            t.layoutInflater = LayoutInflater.from(recyclerView.context)
            t.recycledViewPool = recycledViewPool
            t.itemClickListener = itemClickListener
            t.itemDragListener = itemDragListener

            snapHelper?.attachToRecyclerView(recyclerView)

            recycledViewPool?.let {
                recyclerView.setRecycledViewPool(it)
            }
            layoutManager?.let {
                recyclerView.layoutManager = it
            }
            hasFixedSize?.let {
                recyclerView.setHasFixedSize(it)
            }
            isNestedScrollingEnabled?.let {
                recyclerView.isNestedScrollingEnabled = it
            }
            isDraggable?.let {
                t.setDraggable(it)
            }
            isExpandable?.let {
                t.setExpandable(it)
            }
            isSwipeToDismissEnabled?.let {
                t.setIsSwipeToDismissEnabled(it)
            }
            dividerDrawable.let {
                t.setDivider(it, dividerDrawableInsetLeft, dividerDrawableInsetTop, dividerDrawableInsetRight, dividerDrawableInsetBottom)
            }

            if (set) {
                recyclerView.adapter = t
            }
            return t
        }
    }

    companion object {

        private fun init() {
            if (PrimeAdapterUtils.viewTypeMap.isEmpty()) {
                PrimeAdapterUtils.instantiateViewTypeManager()
            }
        }

        fun with(recyclerView: RecyclerView): AdapterBuilder {
            init()
            return AdapterBuilder(recyclerView)
        }

    }

}