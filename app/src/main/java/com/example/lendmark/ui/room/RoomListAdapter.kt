package com.example.lendmark.ui.room

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lendmark.data.model.Room
import com.example.lendmark.databinding.ItemRoomBinding

class RoomListAdapter(
    private val rooms: List<Room>,
    private val onRoomClick: (Room) -> Unit,
    private val onMoreInfoClick: (Room) -> Unit
) : RecyclerView.Adapter<RoomListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRoomBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]

        with(holder.binding) {
            tvRoomName.text = room.name
            tvCapacity.text = "${room.capacity}명"
            tvFloor.text = room.floor

            // 카드 전체 클릭 → 예약 가능한 시간표로 이동
            root.setOnClickListener { onRoomClick(room) }

            // 하단 “강의실 정보 보기”
            tvMoreInfo.setOnClickListener { onMoreInfoClick(room) }
            ivStar.setOnClickListener { onMoreInfoClick(room) }
        }
    }

    override fun getItemCount() = rooms.size
}
