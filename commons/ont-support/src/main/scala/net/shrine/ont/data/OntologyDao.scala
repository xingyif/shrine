package net.shrine.ont.data

import net.shrine.ont.messaging.Concept

/**
 * @author Dave Ortiz
 * @author Clint Gilbert
 * @date 9/7/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait OntologyDao {
  def ontologyEntries: Iterable[Concept]
}