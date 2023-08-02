package com.example.fibonacci;



import java.util.function.Predicate;
// tag::implementing-action[]
import kalix.javasdk.action.Action;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
// end::implementing-action[]

import static io.grpc.Status.Code.INVALID_ARGUMENT;


// tag::implementing-action[]
@RequestMapping("/fibonacci")
public class FibonacciAction extends Action {

  private boolean isFibonacci(long num) {  // <1>
    Predicate<Long> isPerfectSquare = (n) -> {
      long square = (long) Math.sqrt(n);
      return square * square == n;
    };
    return isPerfectSquare.test(5*num*num + 4) || isPerfectSquare.test(5*num*num - 4);
  }
  private long nextFib(long num) {   // <2>
    double result = num * (1 + Math.sqrt(5)) / 2.0;
    return Math.round(result);
  }

  @GetMapping("/{number}/next")
  public Effect<Number> getNumber(@PathVariable Long number) { // <3>
    return nextNumber(new Number(number));
  }

  @PostMapping("/next")
  public Effect<Number> nextNumber(@RequestBody Number number) {  
    long num =  number.value();
    if (isFibonacci(num)) {                                     // <4>
      return effects().reply(new Number(nextFib(num)));
    } else {
      return effects()                                          // <5>
        .error("Input number is not a Fibonacci number, received '" + num + "'", INVALID_ARGUMENT);
    }
  }
}
// end::implementing-action[]
