class A<T> {

  List<T> x

    Object fo<caret>o(List<T> a) {
    x = a
  }
}
new A<Integer>().foo([1])