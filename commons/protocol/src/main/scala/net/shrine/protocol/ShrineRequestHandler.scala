package net.shrine.protocol

import net.shrine.protocol.handlers.DeleteQueryHandler
import net.shrine.protocol.handlers.ReadApprovedTopicsHandler
import net.shrine.protocol.handlers.ReadInstanceResultsHandler
import net.shrine.protocol.handlers.ReadPdoHandler
import net.shrine.protocol.handlers.ReadPreviousQueriesHandler
import net.shrine.protocol.handlers.ReadQueryDefinitionHandler
import net.shrine.protocol.handlers.ReadQueryInstancesHandler
import net.shrine.protocol.handlers.ReadQueryResultHandler
import net.shrine.protocol.handlers.RenameQueryHandler
import net.shrine.protocol.handlers.RunQueryHandler
import net.shrine.protocol.handlers.ReadTranslatedQueryDefinitionHandler
import net.shrine.protocol.handlers.FlagQueryHandler
import net.shrine.protocol.handlers.UnFlagQueryHandler

/**
 * @author Bill Simons
 * @date 3/9/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait ShrineRequestHandler extends 
	ReadPreviousQueriesHandler[ReadPreviousQueriesRequest, BaseShrineResponse] with
	ReadApprovedTopicsHandler[ReadApprovedQueryTopicsRequest, BaseShrineResponse] with
	ReadQueryInstancesHandler[ReadQueryInstancesRequest, BaseShrineResponse] with 
	ReadInstanceResultsHandler[ReadInstanceResultsRequest, BaseShrineResponse] with
	ReadPdoHandler[ReadPdoRequest, BaseShrineResponse] with 
	ReadQueryDefinitionHandler[ReadQueryDefinitionRequest, BaseShrineResponse] with 
	RunQueryHandler[RunQueryRequest, BaseShrineResponse] with 
	DeleteQueryHandler[DeleteQueryRequest, BaseShrineResponse] with
	RenameQueryHandler[RenameQueryRequest, BaseShrineResponse] with 
	ReadQueryResultHandler[ReadQueryResultRequest, BaseShrineResponse] with
	ReadTranslatedQueryDefinitionHandler[ReadTranslatedQueryDefinitionRequest, BaseShrineResponse] with
	FlagQueryHandler[FlagQueryRequest, BaseShrineResponse] with
	UnFlagQueryHandler[UnFlagQueryRequest, BaseShrineResponse]
	
