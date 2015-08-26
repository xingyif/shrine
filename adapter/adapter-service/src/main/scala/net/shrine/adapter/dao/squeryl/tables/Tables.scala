package net.shrine.adapter.dao.squeryl.tables

import org.squeryl.Schema
import net.shrine.dao.squeryl.SquerylEntryPoint._

final class Tables extends Schema with 
	BreakdownResultsComponent with 
	CountResultsComponent with 
	ErrorResultsComponent with 
	PrivilegedUsersComponent with 
	QueryResultsComponent with 
	ShrineQueriesComponent