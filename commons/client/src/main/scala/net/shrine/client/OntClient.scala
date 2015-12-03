package net.shrine.client

/**
 * @author clint
 * @date Jan 24, 2014
 */
trait OntClient  {
  /**
   * Asks the ont cell for child terms given a parent term, using the
   * ont cell's getChildren operation. 
   * 
   * For example, if the following terms are present:
   * 
   * \Foo
   * \Foo\Bar
   * \Foo\Bar\Baz
   * \Foo\Bar\Blarg
   * \Foo\Nuh
   * \\Foo\Nuh\Zuh
   * 
   * getChildren(\Foo) will give Set(\Foo\Bar,\Foo\Nuh)
   * 
   * getChildren(\Foo\bar) will give Set(\Foo\Bar\Baz,\Foo\Bar\Blarg)
   * 
   * and
   * 
   * getChildren(\Foo\Bar\Baz) will give Set.empty
   */
  def childrenOf(parent: String): Set[String]
}