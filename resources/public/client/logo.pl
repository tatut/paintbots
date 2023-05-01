:- use_module(library(dcg/basics)).
:- set_prolog_flag(double_quotes, chars).

log(Pattern, Args) :-
    format(string(L), Pattern, Args),
    _ := log(L).

%% Simple form data parsing, the builtin HTTP client would do this for us, but
%% we are using text transfer between JS fetch and Prolog ¯\_(ツ)_/¯
form_data([Name=Value | More]) --> string_without("=", NameS), "=", string_without("&", ValueS),
                                   { atom_chars(Name, NameS),
                                     ((Name = x; Name = y) -> number_chars(Value, ValueS); Value = ValueS)
                                   },
                                   more_form_data(More).
more_form_data([]) --> [].
more_form_data(Fd) --> "&", form_data(Fd).

form_data_or_plain(Out) --> form_data(Out) | (string_without("=", Id), { atom_chars(Out, Id) }).

post(FormData, ResultOut) :-
    P := pb_post(FormData),
    await(P, Result),
    TP := Result.text(),
    await(TP, ResultText),
    string_chars(ResultText, Cs),
    phrase(form_data_or_plain(ResultOut), Cs).
    %writeln(got_post_result(ResultOut)).

% Basic DCG state nonterminals
state(S), [S] --> [S].
state(S0, S), [S] --> [S0].

% Push and pop environment bindings
push_env, [S, Env] --> [S], { S = bot(_,_,_,_,t(_,Env,_)) }.
pop_env, [bot(Id,X,Y,C,t(Ang,EnvSaved,PenUp))] -->
    [bot(Id,X,Y,C,t(Ang,_,PenUp)), EnvSaved].

% X,Y position accessor
pos(X,Y) --> state(bot(_,X,Y,_,_)).

%% Issue bot command, update bot state from response
%% The last part of the state is User data which is passed
%% through unchanged.
cmd(Args) -->
    state(bot(Id, _, _, _, User), bot(Id, X, Y, C, User)),
    { post([ id=Id | Args ], Res),
      member(x=Xs, Res),
      member(y=Ys, Res),
      member(color=C, Res),
      atom_to_term(Xs, X, []),
      atom_to_term(Ys, Y, []),
      User = t(Ang, _, _),
      _ := botinfo(X,Y,C, Ang)
    }.

% Calculate points for a line from [X1,Y1] to [X2,Y2]
line([X,Y],[X,Y],[]).
line([X1, Y1], [X2,Y2], Points) :-
    Dx is abs(X1-X2),
    Dy is abs(Y1-Y2),
    (Dx > Dy -> Steps = Dx; Steps = Dy),
    XStep is (X2 - X1) / Steps,
    YStep is (Y2 - Y1) / Steps,
    line_([X1,Y1], [X2,Y2], XStep, YStep, Steps, Points).

% All steps done, return end point
line_(_, [X,Y], _, _, 0, [[X,Y]]).

% More steps to do
line_([Xp,Yp], To, XStep, YStep, Steps, [[X,Y]|Rest]) :-
    X is round(Xp),
    Y is round(Yp),
    Xn is Xp + XStep,
    Yn is Yp + YStep,
    StepsN is Steps - 1,
    line_([Xn,Yn], To, XStep, YStep, StepsN, Rest).

%% Register bot with name, fetches and returns initial state of the bot
register(Name, State) :- register(Name, initial, State).

register(Name, UserData, State) :-
    post([register = Name], Id),
    phrase(cmd([info='']), [bot(Id,_,_,_,UserData)], [State]),
    log('Registered ~w', [State]).

%% Bot commands take form of DCG nonterminals with bot state as final args:
%% command(CommandArgs, S0, S1)
move(Dir) --> cmd([move=Dir]).

paint --> cmd([paint='']).

draw_line(To) -->
    pos(X,Y),
    { line([X,Y], To, Points) },
    traverse(Points).

% Traverse empty points
traverse([]) --> [].

% Traverse 1 point, just paint it
traverse([_]) --> paint.

% Two or more points, paint and move
traverse([_,P2|Points]) -->
    paint,
    move_to(P2),
    traverse([P2|Points]).

% At position, do nothing
move_to([X,Y]) --> pos(X,Y).

% Not at position, move horizontally or vertically
move_to([Xt,Yt]) -->
    pos(Xp,Yp),
    { once(dir([Xp,Yp],[Xt,Yt],Dir)) },
    move(Dir),
    move_to([Xt,Yt]).

bye(bot(Id,_,_,_,_)) :-
    post([id=Id, bye=''], _Out).

say(Msg) --> cmd([msg=Msg]).

color(C) --> cmd([color=C]).

% Determine which direction to go, based on two points
dir([X1,_],[X2,_], 'RIGHT') :- X1 < X2.
dir([X1,_],[X2,_], 'LEFT') :- X1 > X2.
dir([_,Y1],[_,Y2], 'DOWN') :- Y1 < Y2.
dir([_,Y1],[_,Y2], 'UP') :- Y1 > Y2.

%%
%% Turtle graphics DCG
%%

strip_comments([], []).
strip_comments([C],[C]).
strip_comments([(/),(/)|Rest],Out) :- skip_to("\n", Rest, Out1), strip_comments(Out1, Out).
strip_comments([(/),(*)|Rest],Out) :- skip_to("*/", Rest, Out1), strip_comments(Out1, Out).
strip_comments([C1,C2|Rest], Out) :-
    not((C1 = (/), (C2 = (*); C2 = (/)))),
    strip_comments([C2|Rest], RestOut),
    append([C1], RestOut, Out).

skip_to(End, Cs, Out) :-
    append(End, Out, Cs).
skip_to(End, [C|Cs], Out) :-
    not(append(End,Out,[C|Cs])),
    skip_to(End, Cs, Out).


ws --> [W], { char_type(W, space) }, ws.
ws --> [].

% At least one whitespace
ws1 --> [W], { char_type(W, space) }, ws.

parse(Source, Prg) :-
    writeln(parsing(Source)),
    strip_comments(Source, SourceStripped),
    writeln(stripped(SourceStripped)),
    phrase(turtle(Prg), SourceStripped).

turtle([]) --> [].
turtle([P|Ps]) --> ws, turtle_command(P), ws, turtle(Ps).

turtle_command(Cmd) --> defn(Cmd) | fncall(Cmd) |
                        fd(Cmd) | bk(Cmd) | rt(Cmd) |
                        pen(Cmd) | randpen(Cmd) |
                        repeat(Cmd) | setxy(Cmd) |  savexy(Cmd) |  setang(Cmd) |
                        for(Cmd) | say_(Cmd) |
                        pendown(Cmd) | penup(Cmd) | lineto(Cmd).

defn(defn(FnName, ArgNames, Body)) -->
    "def", ws1, ident(FnName), ws, "(", defn_args(ArgNames), ")", ws, "{", turtle(Body), "}".

fncall(fncall(FnName, ArgValues)) --> ident(FnName), ws, "(", fncall_args(ArgValues), ")".
fncall_args([]) --> [].
fncall_args([V|Vs]) --> exprt(V), more_fncall_args(Vs).
more_fncall_args([]) --> ws.
more_fncall_args(Vs) --> ws1, fncall_args(Vs).

ident_([]) --> [].
ident_([I|Is]) --> [I], { char_type(I, csymf) }, ident_(Is).
ident(I) --> ident_(Cs), { atom_chars(I, Cs) }.


defn_args([]) --> [].
defn_args([Arg|Args]) --> ws, ident(Arg), more_defn_args(Args).
more_defn_args([]) --> ws.
more_defn_args(Args) --> ws1, defn_args(Args).

repeat(repeat(Times,Program)) --> "repeat", exprt(Times), "[", turtle(Program), "]".

say_(say(Msg)) --> "say", ws, "\"", string_without("\"", Codes), "\"", { atom_codes(Msg, Codes) }.
fd(fd(N)) --> "fd", exprt(N).
bk(bk(N)) --> "bk", exprt(N).
rt(rt(N)) --> "rt", exprt(N).
pen(pen(Col)) --> "pen", ws, [Col], { char_type(Col, alnum) }, ws.
randpen(randpen) --> "randpen".
setxy(setxy(X,Y)) --> "setxy", exprt(X), exprt(Y).
savexy(savexy(X,Y)) --> "savexy", ws, ident(X), ws1, ident(Y).
lineto(lineto(X,Y)) --> "line", exprt(X), exprt(Y).
setang(setang(Deg)) --> "setang", exprt(Deg).
setang(setang(X,Y)) --> "angto", exprt(X), exprt(Y).
for(for(Var, From, To, Step, Program)) -->
    "for", ws, "[", ws, ident(Var), exprt(From), exprt(To), exprt(Step), "]", ws,
    "[", turtle(Program), "]".
num(N) --> "-", num_(I), { N is -I }.
num(N) --> num_(N).
num_(N) --> integer(N).
num_(F) --> digits(IP), ".", digits(FP), { append(IP, ['.'|FP], Term),  read_from_chars(Term, F) }.
arg_(num(N)) --> num(N).
arg_(var(V)) --> ":", ident(V).
arg_(rnd(Low,High)) --> "rnd", ws, num(Low), ws, num(High).
penup(penup) --> "pu" | "penup".
pendown(pendown) --> "pd" | "pendown".

% Parse simple math expression tree. There is no priority for multipliation and addition.
% Use parenthesis to change order.

exprt(E) --> ws, expr(E), ws. % top level, wrap with whitespace

expr(A) --> arg_(A).
expr(E) --> "(", exprt(E), ")".
expr(op(Left,Op,Right)) --> expr_left(Left), ws, op_(Op), exprt(Right).

expr_left(E) --> "(", exprt(E), ")".
expr_left(A) --> arg_(A).

op_(*) --> "*".
op_(/) --> "/".
op_(+) --> "+".
op_(-) --> "-".

% Parse a turtle program:
% set_prolog_flag(double_quote, chars).
% phrase(turtle(Program), "fd 6 rt 90 fd 6").


% Interpreting a turtle program.
%
% state is a compound term of:
% t(Angle, Env, PenUp)
% where Env is a dict of the current env bindings (functions and arguments)
% and PenUp is true/false atom if pen is up (should not draw when moving)

eval_turtle(Name, Program) :-
    setup_call_cleanup(
        register(Name, t(0, env{}, false), B0),
        phrase(eval_all(Program), [B0], [_]),
        bye(B0)).

eval_all([]) --> [].
eval_all([Cmd|Cmds]) -->
    eval(Cmd),
    eval_all(Cmds).

deg_rad(Deg, Rad) :-
    Rad is Deg * pi/180.

rad_deg(Rad, Deg) :-
    Deg is Rad / pi * 180.

user_data(Old, New) -->
    state(bot(Id,X,Y,C,Old), bot(Id,X,Y,C,New)).

user_data(Current) -->
    state(bot(_,_,_,_,Current)).

set_angle(A) -->
    user_data(t(_,Env,PenUp), t(A,Env,PenUp)).

set_pen_up -->
    user_data(t(Ang,Env,_), t(Ang,Env,true)).

set_pen_down -->
    user_data(t(Ang,Env,_), t(Ang,Env,false)).

%% Eval argument against current ctx, var is taken from dictionary
%% numbers are evaluated as is.
%%
%% Argument can be a math expression op as eval.

argv(var(V), Val) -->
    user_data(t(_,Env,_)),
    { Val = Env.V }.

argv(num(V), V) --> [].

argv(rnd(Low,High), V) --> { random_between(Low,High,V) }.

argv(op(Left_,Op,Right_), V) -->
    argv(Left_, Left),
    argv(Right_, Right),
    { eval_op(Left, Op, Right, V) }.

eval_op(L,+,R,V) :- V is L + R.
eval_op(L,-,R,V) :- V is L - R.
eval_op(L,*,R,V) :- V is L * R.
eval_op(L,/,R,V) :- V is L / R.

setval(Var, Val) -->
    user_data(t(Ang,Env0,PenUp), t(Ang,Env1,PenUp)),
    { Env1 = Env0.put(Var, Val) }.

setargs([],[]) --> [].
setargs([K|Ks], []) --> { throw(error(not_enough_arguments, missing_vars([K|Ks]))) }.
setargs([], [V|Vs]) --> { throw(error(too_many_arguments, extra_values([V|Vs]))) }.
setargs([K|Ks], [V|Vs]) -->
    argv(V, Val),
    setval(K, Val), setargs(Ks,Vs).


% Moving with pen up or down
move_forward(true, Pos) --> move_to(Pos).
move_forward(false, Pos) --> draw_line(Pos).

eval(rt(DegArg)) -->
    argv(DegArg, Deg),
    user_data(t(Ang0, Env, PenUp), t(Ang1, Env, PenUp)),
    { Ang1 is (Ang0 + Deg) mod 360 }.

eval(fd(LenArg)) -->
    argv(LenArg, Len),
    state(bot(_,X,Y,_,t(Ang,_,PenUp))),
    { deg_rad(Ang, Rad),
      X1 is round(X + Len * cos(Rad)),
      Y1 is round(Y + Len * sin(Rad)) },
    move_forward(PenUp, [X1, Y1]).

eval(bk(LenArg)) -->
    argv(LenArg, Len),
    { MinusLen is -Len },
    eval(fd(num(MinusLen))).

eval(pen(C)) --> color(C).
eval(randpen) -->
    { C is random(16), format(atom(Col), '~16r', [C]) },
    color(Col).

eval(repeat(num(0), _)) --> [].
eval(repeat(NArg, Cmds)) -->
    argv(NArg, N),
    { N > 0,
      N1 is N - 1 },
    eval_all(Cmds),
    eval(repeat(num(N1), Cmds)).

eval(setxy(XArg,YArg)) -->
    argv(XArg, X_), argv(YArg, Y_),
    { X is round(X_), Y is round(Y_) },
    move_to([X,Y]).

eval(savexy(XVar,YVar)) -->
    pos(X, Y),
    setval(XVar, X),
    setval(YVar, Y).

eval(setang(AngArg)) -->
    argv(AngArg, Ang),
    set_angle(Ang).

% Set angle towards a target point
eval(setang(TargetX_,TargetY_)) -->
    pos(PosX,PosY),
    argv(TargetX_, TargetX),
    argv(TargetY_, TargetY),
    set_angle(Deg),
    { Ang is atan2(TargetY-PosY, TargetX-PosX),
      rad_deg(Ang, Deg0),
      Deg is round(Deg0),
      writeln(angle(to(TargetX,TargetY),from(PosX,PosY),rad_deg(Ang,Deg)))
    }.

eval(penup) --> set_pen_up.
eval(pendown) --> set_pen_down.

%% Loop done
eval(for_(_, From, To, Step, _)) -->
    { (Step > 0, From > To); (Step < 0, From < To) }, [].

eval(for_(Var, From, To, Step, Program)) -->
    setval(Var, From),
    eval_all(Program),
    { From1 is From + Step },
    eval(for_(Var, From1, To, Step, Program)).

eval(for(Var, From_, To_, Step_, Program)) -->
    argv(From_, From),
    argv(To_, To),
    argv(Step_, Step),
    eval(for_(Var, From, To, Step, Program)).


eval(say(Msg)) -->
    say(Msg).

eval(defn(FnName, ArgNames, Body)) -->
    setval(FnName, fn(ArgNames,Body)).

eval(fncall(FnName, ArgValues)) -->
    push_env,
    user_data(t(_,Env,_)),
    { fn(ArgNames,Body) = Env.FnName },
    setargs(ArgNames, ArgValues),
    eval_all(Body),
    pop_env.

eval(lineto(X_, Y_)) -->
    argv(X_, Xf),
    argv(Y_, Yf),
    { X is round(Xf), Y is round(Yf) },
    draw_line([X,Y]).


run(Name, Program) :-
    phrase(turtle(P), Program),
    eval_turtle(Name, P).

logo(Name) :-
    log('toy Logo repl, registering bot "~w"\n', [Name]),
    register(Name, t(0, env{}, false), Bot0),
    logo_repl(Bot0, BotF),
    bye(BotF).

exec(Program, Bot0, BotOut) :-
    catch((log('Executing program',[]),
           writeln(program(Program)),
           call_time(phrase(eval_all(Program), [Bot0], [Bot1]), Time),
           log('DONE in ~1f seconds', [Time.wall]),
           BotOut = Bot1),
          error(Error,ErrCtx),
          (log('ERROR: ~w (~w)', [Error, ErrCtx]),
           phrase(cmd([info='']), [Bot0], [BotOut]))).


logo_repl(Bot0, BotF) :-
    InputPromise := get_input(),
    await(InputPromise, Str),
    string_chars(Str, Cs),
    ( (Cs = "bye"; Str = end_of_file) ->
      log('Bye!',[]),
      Bot0 = BotF
    ; ( parse(Cs, Program) ->
        exec(Program, Bot0, Bot1),
        logo_repl(Bot1, BotF)
      ; log('Syntax error in: ~w', [Str]),
        logo_repl(Bot0, BotF))).

start_repl :-
    NameStr := get_bot_name(),
    atom_string(Name, NameStr),
    Name \= '',
    logo(Name).


slurp_file(F, L) :-
  setup_call_cleanup(
      open(F, read, In),
      slurp(In, L),
      close(In)
  ).

slurp(In, L):-
  read_line_to_string(In, Line),
  (   Line == end_of_file
  ->  L = []
  ;   string_chars(Line, LineCs),
      append(LineCs, Lines, L),
      slurp(In,Lines)
  ).
