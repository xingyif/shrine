package net.shrine.jersey

import net.shrine.wiring.ShrineOrchestrator

/**
 * @author clint
 * @since Jan 16, 2014
 * 
 * Default (non-HMS) Shrine "entry point" for Jersey.
 */
final class DefaultShrineResourceConfig extends ShrineResourceConfig(ShrineOrchestrator)