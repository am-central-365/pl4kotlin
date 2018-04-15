package com.amcentral365.pl4kotlin

import com.google.common.annotations.VisibleForTesting
import mu.KLogging
import java.sql.Connection


open class InsertStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    companion object: KLogging()

    @VisibleForTesting internal val fetchDescr:  MutableList<Descr>         = mutableListOf()
    @VisibleForTesting internal val bindColDefs: MutableList<Entity.ColDef> = mutableListOf()

    override fun run(conn: Connection): Int = this.runDML(conn, this.bindColDefs, this.fetchDescr)

    /**
     * Override BaseStatemen's bind() to honor bind value generation.
     * Unlike other
    override fun bind(stmt: PreparedStatement, vals: List<Any?>) =
        // we KNOW vals is this.bindColDefs: we've just passed it to run() which has called us
        this.bindColDefs.forEachIndexed { k, v -> this.bind(stmt, k+1, v.getBindValue()) }
     */

    override fun build(): String {
        this.bindColDefs.clear()
        this.fetchDescr.clear()

        val sb = StringBuilder("INSERT INTO ").append(this.entityDef.tableName).append('(')
        var sep = ""
        for( colDef in this.entityDef.colDefs ) {
            val useDefault =  colDef.onInsert === Generated.OnTheDbAlways
                          || (colDef.onInsert === Generated.OnTheDbWhenNull && colDef.getValue() == null)
            if( useDefault ) {
                this.fetchDescr.add(Descr(colDef))
            } else {
                sb.append(sep).append(colDef.columnName)
                this.bindColDefs.add(colDef)
                sep = ", "
            }
        }

        sb.append(") VALUES(")
          .append(String(CharArray(this.bindColDefs.size)).replace("\u0000", ", ?").substring(2))
          .append(')')

        return sb.toString()
    }

}