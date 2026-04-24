package com.source1.deliveryagent.Adapter
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.source1.deliveryagent.R
import com.source1.deliveryagent.model.OrderModel

class OrderAdapter(
    private val list: ArrayList<OrderModel>,
    private val context: Context
) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.customerName)
        val address = itemView.findViewById<TextView>(R.id.address)
        val amount = itemView.findViewById<TextView>(R.id.amount)
        val status = itemView.findViewById<TextView>(R.id.status)
        val callBtn = itemView.findViewById<Button>(R.id.callBtn)
        val deliverBtn = itemView.findViewById<Button>(R.id.deliverBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val order = list[position]

        holder.name.text = order.userName
        holder.address.text = order.address
        holder.amount.text = "₹${order.totalPrice}"
        holder.status.text = order.status

        // 📞 CALL
        holder.callBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${order.phoneNumber}")
            context.startActivity(intent)
        }

        // ✅ MARK DELIVERED
        holder.deliverBtn.setOnClickListener {

            val db = FirebaseDatabase.getInstance().reference
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            db.child("Orders").child(order.itemPushKey!!).child("status")
                .setValue("delivered")

            db.child("DeliveryBoys").child(userId).child("deliveredOrders")
                .setValue(ServerValue.increment(1))

            db.child("DeliveryBoys").child(userId).child("activeDrops")
                .setValue(ServerValue.increment(-1))
        }
    }
}