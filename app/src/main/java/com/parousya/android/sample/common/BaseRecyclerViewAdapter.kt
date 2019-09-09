package com.parousya.android.sample.common

import androidx.recyclerview.widget.RecyclerView


abstract class BaseRecyclerViewAdapter<V, VH> : RecyclerView.Adapter<VH>() where VH : RecyclerView.ViewHolder {

    protected var items: ArrayList<V>? = null

    override fun getItemCount() = items?.size ?: 0

    fun set(items: List<V>?) {
        items?.let {
            this.items = ArrayList(it)
            notifyDataSetChanged()
        }
    }

    fun addAll(list: List<V>?) {
        list?.let {
            items?.addAll(it)
            notifyDataSetChanged()
        }
    }

    fun removeItem(item: V) {
        items?.let {
            val index = it.indexOf(item)
            if (index != -1) {
                it.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    fun updateItem(oldItem: V, newItem: V? = null) {
        items?.let {
            val index = it.indexOf(oldItem)
            if (index != -1) {
                if (newItem != null) {
                    it[index] = newItem
                }
                notifyItemChanged(index)
            }
        }
    }

    fun addItem(item: V) {
        items?.let {
            it.add(item)
            notifyItemInserted(it.size - 1)
        }
    }

    fun addItemAsFirst(item: V) {
        items?.let {
            it.add(0, item)
            notifyItemInserted(it.size - 1)
        }
    }

    fun clear() {
        items?.let {
            it.clear()
            notifyItemRangeRemoved(0, it.size)
        }
    }
}