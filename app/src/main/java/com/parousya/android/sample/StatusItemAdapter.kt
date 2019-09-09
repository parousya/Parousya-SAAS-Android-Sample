package com.parousya.android.sample

import androidx.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.parousya.android.sample.common.BaseRecyclerViewAdapter
import kotlinx.android.synthetic.main.item_status.view.*

class StatusItemAdapter : BaseRecyclerViewAdapter<Status, StatusItemAdapter.StatusHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusHolder {
        val inflatedView = parent.inflate(R.layout.item_status, false)
        return StatusHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: StatusHolder, position: Int) {
        holder.bind(items!![position])
    }

    inner class StatusHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var status: Status? = null

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {

        }

        fun bind(status: Status) = with(itemView) {
            this@StatusHolder.status = status
            tvContent.text = status.title
        }
    }
}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}