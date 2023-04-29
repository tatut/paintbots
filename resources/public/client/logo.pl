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
push_state, [S, S] --> [S].
pop_state, [S] --> [_, S].

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
      _ := botinfo(X,Y,C, User.angle)
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


ws --> [W], { char_type(W, space) }, ws.
ws --> [].

% At least one whitespace
ws1 --> [W], { char_type(W, space) }, ws.

turtle([]) --> [].
turtle([P|Ps]) --> ws, turtle_command(P), ws, turtle(Ps).

turtle_command(Cmd) --> defn(Cmd) | fncall(Cmd) |
                        fd(Cmd) | bk(Cmd) | rt(Cmd) |
                        pen(Cmd) | randpen(Cmd) |
                        repeat(Cmd) | setxy(Cmd) | setang(Cmd) |
                        for(Cmd) | say_(Cmd).

defn(defn(FnName, ArgNames, Body)) -->
    "def", ws1, ident(FnName), ws, "(", defn_args(ArgNames), ")", ws, "{", turtle(Body), "}".

fncall(fncall(FnName, ArgValues)) --> ident(FnName), ws, "(", fncall_args(ArgValues), ")".
fncall_args([]) --> [].
fncall_args([V|Vs]) --> arg_(V), more_fncall_args(Vs).
more_fncall_args([]) --> ws.
more_fncall_args(Vs) --> ws1, fncall_args(Vs).

ident_([]) --> [].
ident_([I|Is]) --> [I], { char_type(I, csymf) }, ident_(Is).
ident(I) --> ident_(Cs), { atom_chars(I, Cs) }.


defn_args([]) --> [].
defn_args([Arg|Args]) --> ws, ident(Arg), more_defn_args(Args).
more_defn_args([]) --> ws.
more_defn_args(Args) --> ws1, defn_args(Args).

repeat(repeat(Times,Program)) --> "repeat", ws, arg_(Times), ws, "[", turtle(Program), "]".

say_(say(Msg)) --> "say", ws, "\"", string_without("\"", Codes), "\"", { atom_codes(Msg, Codes) }.
fd(fd(N)) --> "fd", ws, arg_(N).
bk(bk(N)) --> "bk", ws, arg_(N).
rt(rt(N)) --> "rt", ws, arg_(N).
pen(pen(Col)) --> "pen", ws, [Col], { char_type(Col, alnum) }, ws.
randpen(randpen) --> "randpen".
setxy(setxy(X,Y)) --> "setxy", ws, arg_(X), ws, arg_(Y).
setang(setang(Deg)) --> "setang", ws, arg_(Deg).
for(for(Var, From, To, Step, Program)) -->
    "for", ws, "[", ws, ident(Var), ws, num(From), ws, num(To), ws, num(Step), ws, "]", ws,
    "[", turtle(Program), "]".
num(N) --> "-", integer(I), { N is -I }.
num(N) --> integer(N).
arg_(num(N)) --> num(N).
arg_(var(V)) --> ":", ident(V).
arg_(rnd(Low,High)) --> "rnd", ws, num(Low), ws, num(High).

% Parse a turtle program:
% set_prolog_flag(double_quote, chars).
% phrase(turtle(Program), "fd 6 rt 90 fd 6").


% Interpreting a turtle program.

eval_turtle(Name, Program) :-
    setup_call_cleanup(
        register(Name, ctx{angle: 0}, B0),
        phrase(eval_all(Program), [B0], [_]),
        bye(B0)).

eval_all([]) --> [].
eval_all([Cmd|Cmds]) -->
    eval(Cmd),
    eval_all(Cmds).

deg_rad(Deg, Rad) :-
    Rad is Deg * pi/180.

user_data(Old, New) -->
    state(bot(Id,X,Y,C,Old), bot(Id,X,Y,C,New)).

user_data(Current) -->
    state(bot(_,_,_,_,Current)).

%% Eval argument against current ctx, var is taken from dictionary
%% numbers are evaluated as is.
argv(var(V), Val) -->
    user_data(Ctx),
    { Val = Ctx.V }.

argv(num(V), V) --> [].

argv(rnd(Low,High), V) --> { random_between(Low,High,V) }.

setval(Var, Val) -->
    user_data(Ctx0, Ctx1),
    { Ctx1 = Ctx0.put(Var, Val) }.

setargs([],[]) --> [].
setargs([K|Ks], [V|Vs]) -->
    argv(V, Val),
    setval(K, Val), setargs(Ks,Vs).

eval(rt(DegArg)) -->
    argv(DegArg, Deg),
    user_data(Ctx0, Ctx1),
    { Ang0 = Ctx0.angle,
      Ang1 is (Ang0 + Deg) mod 360,
      Ctx1 = Ctx0.put(angle, Ang1) }.

eval(fd(LenArg)) -->
    argv(LenArg, Len),
    state(bot(_,X,Y,_,Ctx)),
    { deg_rad(Ctx.angle, Rad),
      X1 is round(X + Len * cos(Rad)),
      Y1 is round(Y + Len * sin(Rad)) },
    draw_line([X1,Y1]).

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
    argv(XArg, X), argv(YArg, Y),
    move_to([X,Y]).

eval(setang(AngArg)) -->
    argv(AngArg, Ang),
    setval(angle, Ang).

%% Loop done
eval(for(_, From, To, Step, _)) -->
    { (Step > 0, From > To); (Step < 0, From < To) }, [].

eval(for(Var, From, To, Step, Program)) -->
    setval(Var, From),
    eval_all(Program),
    { From1 is From + Step },
    eval(for(Var, From1, To, Step, Program)).

eval(say(Msg)) -->
    say(Msg).

eval(defn(FnName, ArgNames, Body)) -->
    { writeln(eval_defn(FnName)) },
    setval(FnName, fn(ArgNames,Body)).

% PENDING: don't push/pop state, as that restores bot position
% we could save the angle, but otherwise push/pop the context.
eval(fncall(FnName, ArgValues)) -->
    %push_state,
    user_data(Ctx),
    { writeln(context_before(Ctx)),
        fn(ArgNames,Body) = Ctx.FnName },
    setargs(ArgNames, ArgValues),
    eval_all(Body).
    %pop_state.



run(Name, Program) :-
    phrase(turtle(P), Program),
    eval_turtle(Name, P).

logo(Name) :-
    log('toy Logo repl, registering bot "~w"\n', [Name]),
    register(Name, ctx{angle: 0}, Bot0),
    logo_repl(Bot0, BotF),
    bye(BotF).

logo_repl(Bot0, BotF) :-
    InputPromise := get_input(),
    await(InputPromise, Str),
    string_chars(Str, Cs),
    ( (Cs = "bye"; Str = end_of_file) ->
      log('Bye!',[]),
      Bot0 = BotF
    ; ( phrase(turtle(Program), Cs) ->
        log('Executing program',[]),
        call_time(phrase(eval_all(Program), [Bot0], [Bot1]), Time),
        log('DONE in ~1f seconds', [Time.wall]),
        logo_repl(Bot1, BotF)
      ; log('Syntax error in: ~w', [Str]),
        logo_repl(Bot0, BotF))).

start_repl :-
    NameStr := get_bot_name(),
    atom_string(Name, NameStr),
    Name \= '',
    logo(Name).
