package com.amcentral365.pl4kotlin

import mu.KLogging
import java.sql.Connection
import kotlin.reflect.KProperty

/** Run SQL `UPDATE` statement for the given [entityDef]. See [BaseStatement] constructor for parameters description. */
open class UpdateStatement(entityDef: Entity, getGoodConnection: () -> Connection? = { null }): BaseStatement(entityDef, getGoodConnection) {
    private companion object: KLogging()  /** @suppress */

    /*@VisibleForTesting*/ internal val updateDescrs:    MutableList<BaseStatement.Descr> = mutableListOf()
    /*@VisibleForTesting*/ internal val whereDescrs:     MutableList<BaseStatement.Descr> = mutableListOf()
    /*@VisibleForTesting*/ internal val fetchbackDescrs: MutableList<BaseStatement.Descr> = mutableListOf()
    /*@VisibleForTesting*/ internal val bindVals:        MutableList<Any?> = mutableListOf()

    override fun run(conn: Connection): Int = this.runDML(conn, this.bindVals, this.fetchbackDescrs)

    // ----- Set columns and expressions

    /** Get property's column name and emit `UPDATE ... SET columnName = ?` to the SQL text. On [run], the property value is written to the database */
    fun update(prop:  KProperty<Any?>): UpdateStatement { this.addProperty(this.updateDescrs,  prop);  return this }

    /**
     * Get property's column name and emit `UPDATE ... SET columnName = [expr]` to the SQL text.
     * The expression variables values are via [binds]
     * Note: []targetProp] is only used to determine the target column name. Its value is not used.
     * */
    fun update(targetProp: KProperty<Any?>, expr: String, vararg binds: Any?): UpdateStatement   // set col = expr(binds)
        { this.addProperty(this.updateDescrs, targetProp, expr, *binds);  return this }

    /** Emit `UPDATE ... SET [colName] = ?` to the SQL text. On [run], the property value is written to the database */
    fun update(colName: String): UpdateStatement { this.addColName(this.updateDescrs, colName);  return this }

    /** Emit `UPDATE ... SET columnName = [expr]` to the SQL text. The expression variables values are via [binds] */
    fun update(colName: String, expr: String, vararg binds: Any?): UpdateStatement   // set col = expr(binds)
        { this.addColName(this.updateDescrs, colName, expr, *binds);  return this }

    /** Allows specifying Column Groups like `Entity.allColsButPk`. See documentation for details. */
    fun update(colDefs: List<Entity.ColDef>): UpdateStatement { this.updateDescrs.addAll(colDefs.map { Descr(it) });  return this }


    // ----- WHERE columns or expressions
    // Beside frequently used PK/OptLock expressions, there are two other forms:
    // 1) property or column: translates to "WHERE column = ?", and the bind value comes from the property
    // 2) a free-form expressions with bind values. Bind values provided are computed and saved.

    /** Add Primary Key columns to `WHERE` clause of the UPDATE statement */
    fun byPk(): UpdateStatement
        { this.whereDescrs.addAll(this.entityDef.pkCols.map { Descr(it) });  return this }

    /** Add Primary Key and, if defined, the Optimistic Lock column to `WHERE` clause of the `UPDATE` statement */
    fun byPkAndOptLock():  UpdateStatement
        { this.whereDescrs.addAll(this.entityDef.pkAndOptLockCols.map { Descr(it) });  return this }

    // --- Individual column conditions

    /** Add text `colName = ?` to the statement's `WHERE` clause. `colName` is figured from the passed [prop] */
    fun by(prop:  KProperty<Any?>): UpdateStatement
        { this.addProperty(this.whereDescrs, prop);  return this }

    /** Add text `colName = ?` to the statement's `WHERE` clause. [colName] must be defined in the [Column] annotation. */
    fun by(colName: String): UpdateStatement
        { this.addColName (this.whereDescrs, colName);  return this }

    // --- Free form clause, allowing to specify expressions and use any column

    /** Add [expr] text to the statement's `WHERE` clause. The expression may use bind variable placeholders, the values are passed in [binds] */
    fun by(expr: String, vararg binds: Any?): UpdateStatement
        { this.addColName(this.whereDescrs, null, expr, *binds);  return this }


    // ----- FETCHBACK columns
    // These columns are read back after update, by the Pk (adding OptLock is redundant because we are in the same transaction)

    /**
     * After update, read back the column value into [prop]
     *
     * If fetchback column(s) were requested and `UPDATE` affected one or more rows,
     * `run` fetches value of the column associated with the property. The `SELECT` statement
     * runs in the same transaction. See [SelectStatement.select] for the role of [expr] and [binds].
     *
     * Fetchbacks are used to read values, computed on the database side.
     */
    fun fetchBack(prop: KProperty<Any?>, expr: String? = null, vararg binds: Any?): UpdateStatement
        { this.addProperty(this.fetchbackDescrs,    prop, expr, *binds);  return this }

    /**
     * After update, read back the column [colName] value into its corresponding property
     *
     * If fetchback column(s) were requested and `UPDATE` affected one or more rows,
     * `run` fetches value of the column associated with the property. The `SELECT` statement
     * runs in the same transaction. See [SelectStatement.select] for the role of [expr] and [binds].
     *
     * Fetchbacks are used to read values, computed on the database side.
     */
    fun fetchBack(colName: String, expr: String? = null, vararg binds: Any?): UpdateStatement
        { this.addColName (this.fetchbackDescrs, colName, expr, *binds);  return this }

    public override fun build(): String {
        require(this.updateDescrs.isNotEmpty()) { "${this::class.java.name}(${this.entityDef.tableName}): no columns to update" }
        this.bindVals.clear()
        val sb = StringBuilder("UPDATE ").append(this.entityDef.tableName).append(" SET ")

        sb.append(
            this.updateDescrs.joinToString(", ") {
                it.colDef!!.columnName + " = " +
                    if( it.expr == null ) {
                        this.bindVals.add(it.colDef)
                        "?"
                    } else {
                        this.bindVals.addAll(it.binds)
                        it.expr
                    }
            }
        )

        if( this.whereDescrs.isNotEmpty() ) {
            sb.append(" WHERE ")
            sb.append(this.emitWhereList(this.whereDescrs))
            this.whereDescrs.forEach {
                if( it.expr == null ) bindVals.add(it.colDef)
                else                  bindVals.addAll(it.binds)
            }
        }

        return sb.toString()
    }

}