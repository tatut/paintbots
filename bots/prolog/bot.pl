:- use_module(library(http/http_client)).
:- use_module(library(dcg/basics)).
:- set_prolog_flag(double_quotes, chars).


url(URL) :- getenv("PAINTBOTS_URL", URL) ; URL='http://localhost:31173'.

post(FormData, Result) :-
     url(URL),
     http_post(URL, form(FormData), Result, []).

% Basic DCG state nonterminals
state(S), [S] --> [S].
state(S0, S), [S] --> [S0].

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
      atom_to_term(Ys, Y, []) }.

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
    writeln(registered(Name,State)).

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

line_demo(Name) :-
    register(Name, S0),
    phrase(draw_line([80, 50]), [S0], [S1]),
    bye(S1).

circle_demo(Name) :-
    register(Name, S0),
    S0 = bot(_, X,Y, _),
    draw_circle(S0, [X, Y], 10, 0, 0.1, S1),
    bye(S1).

draw_circle(_, _, Ang, _) -->
    { Max is 2*pi,
      Ang >= Max },
    [].


draw_circle([Xc, Yc], R, Ang, AngStep) -->
    { X is round(Xc + R * cos(Ang)),
      Y is round(Yc + R * sin(Ang)) },
    move_to([X,Y]),
    paint,
    { AngN is Ang + AngStep },
    draw_circle([Xc, Yc], R, AngN, AngStep).

peace_sign -->
    say('Peace to the world!'),
    move_to([80, 20]),
    draw_line([80,50]),
    draw_line([57,68]),
    move_to([80,50]),
    draw_line([103,68]),
    move_to([80,80]),
    draw_line([80,50]),
    draw_circle([80, 50], 30, 0, 0.05).

%% Draw the universal peace symbol in the center
peace(Name) :-
    register(Name, S0),
    phrase(peace_sign, [S0], [SFinal]),
    bye(SFinal).


%%
%% Turtle graphics DCG
%%


ws --> [W], { char_type(W, space) }, ws.
ws --> [].

turtle([]) --> [].
turtle([P|Ps]) --> ws, turtle_command(P), ws, turtle(Ps).

turtle_command(Cmd) --> fd(Cmd) | bk(Cmd) | rt(Cmd) | pen(Cmd) | randpen(Cmd) | repeat(Cmd) | setxy(Cmd).

repeat(Cmd) --> "repeat", ws, num(Times), ws, "[", turtle(Program), "]",
                { Cmd = repeat(Times, Program) }.

fd(fd(N)) --> "fd", ws, num(N).
bk(bk(N)) --> "bk", ws, num(N).
rt(rt(N)) --> "rt", ws, num(N).
pen(pen(Col)) --> "pen", ws, [Col], { char_type(Col, alnum) }, ws.
randpen(randpen) --> "randpen".
setxy(setxy(X,Y)) --> "setxy", ws, num(X), ws, num(Y).

num(N) --> "-", integer(I), { N is -I }.
num(N) --> integer(N).

% Parse a turtle program:
% set_prolog_flag(double_quote, chars).
% phrase(turtle(Program), "fd 6 rt 90 fd 6").


% Interpreting a turtle program.
% The state is a compound term turtle(BotState, AngleDegree)

eval_turtle(Name, Program) :-
    setup_call_cleanup(
        register(Name, 0, B0),
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

eval(rt(Deg)) -->
    user_data(Ang0, Ang1),
    { Ang1 is Ang0 + Deg }.

eval(fd(Len)) -->
    state(bot(_,X,Y,_,Ang)),
    { deg_rad(Ang, Rad),
      X1 is round(X + Len * cos(Rad)),
      Y1 is round(Y + Len * sin(Rad)) },
    draw_line([X1,Y1]).

eval(bk(Len)) -->
    { MinusLen is -Len },
    eval(fd(MinusLen)).

eval(pen(C)) --> color(C).
eval(randpen) -->
    { C is random(16), format(atom(Col), '~16r', [C]) },
    color(Col).

eval(repeat(0, _)) --> [].
eval(repeat(N, Cmds)) -->
    { N > 0,
      N1 is N - 1 },
    eval_all(Cmds),
    eval(repeat(N1, Cmds)).

eval(setxy(X,Y)) --> move_to([X,Y]).

%% phrase(turtle(T), "fd 5 rt 90 fd 5 rt 90 fd 5")
% eval_turtle('Turtles3', [repeat(10,[rt(50),fd(10)])]).

run(Name, Program) :-
    phrase(turtle(P), Program),
    eval_turtle(Name, P).

%% dahlia.logo
%% see http://www.mathcats.com/gallery/15wordcontest.html
dahlia() :-
    run('Dahlia', "setxy 100 10 repeat 8 [rt 45 repeat 6 [repeat 90 [fd 2 rt 2] rt 90]]").

%% Draw a simple star
star() :-
    run('Star', "repeat 5 [ fd 25 rt 144 ]").

stars() :-
    run('Stars', "repeat 6 [ randpen repeat 5 [ fd 25 rt 144 ] fd 30 rt 60]").
