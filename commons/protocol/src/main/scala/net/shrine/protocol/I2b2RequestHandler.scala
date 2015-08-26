package net.shrine.protocol

import net.shrine.protocol.handlers.ReadPreviousQueriesHandler
import net.shrine.protocol.handlers.ReadApprovedTopicsHandler
import net.shrine.protocol.handlers.ReadTranslatedQueryDefinitionHandler
import net.shrine.protocol.handlers.ReadInstanceResultsHandler
import net.shrine.protocol.handlers.RunQueryHandler
import net.shrine.protocol.handlers.DeleteQueryHandler
import net.shrine.protocol.handlers.ReadPdoHandler
import net.shrine.protocol.handlers.RenameQueryHandler
import net.shrine.protocol.handlers.ReadQueryDefinitionHandler
import net.shrine.protocol.handlers.ReadQueryResultHandler
import net.shrine.protocol.handlers.ReadQueryInstancesHandler
import net.shrine.protocol.handlers.FlagQueryHandler
import net.shrine.protocol.handlers.UnFlagQueryHandler
import net.shrine.protocol.handlers.ReadResultOutputTypesHandler

/**
 * @author clint
 * @date Feb 19, 2014
 */
trait I2b2RequestHandler extends 
	ReadPreviousQueriesHandler[ReadPreviousQueriesRequest, ShrineResponse] with
	ReadApprovedTopicsHandler[ReadApprovedQueryTopicsRequest, ShrineResponse] with
	ReadQueryInstancesHandler[ReadQueryInstancesRequest, ShrineResponse] with 
	ReadInstanceResultsHandler[ReadInstanceResultsRequest, ShrineResponse] with
	ReadPdoHandler[ReadPdoRequest, ShrineResponse] with 
	ReadQueryDefinitionHandler[ReadQueryDefinitionRequest, ShrineResponse] with 
	RunQueryHandler[RunQueryRequest, ShrineResponse] with 
	DeleteQueryHandler[DeleteQueryRequest, ShrineResponse] with
	RenameQueryHandler[RenameQueryRequest, ShrineResponse] with
	FlagQueryHandler[FlagQueryRequest, ShrineResponse] with
	UnFlagQueryHandler[UnFlagQueryRequest, ShrineResponse] with 
	ReadResultOutputTypesHandler[ReadResultOutputTypesRequest, ShrineResponse]
